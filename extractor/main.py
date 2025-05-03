import os
import json
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from google import genai
import asyncio
from aiokafka import AIOKafkaConsumer, AIOKafkaProducer

class DocumentContent(BaseModel):
    content: str
    document_id: str

app = FastAPI(title="Extractor Service")

API_KEY = os.getenv("GENAI_API_KEY")
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
KAFKA_CONSUMER_TOPIC = "extraction"
KAFKA_PRODUCER_TOPIC = "extraction_response"

# Kafka consumer and producer
producer = None
consumer = None

# init Gemini client
if not API_KEY:
    print("Warning: GENAI_API_KEY not set - extraction will fail!")
    


print(f"Using GENAI_API_KEY: {API_KEY}")
client = genai.Client(api_key=API_KEY)

@app.post("/extract")
async def extract(doc: DocumentContent):
    """HTTP endpoint to manually trigger extraction"""
    try:
        result = await process_document(doc)
        return result
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Extraction error: {str(e)}")

async def process_document(doc: DocumentContent):

    """Process document content through Gemini API"""
    prompt = (
        "Reformat the following document content into a well‑structured JSON:\n\n"
        f"{doc.content}\n\n"
        "Return ONLY valid JSON."
    )
    try:
        response = client.models.generate_content(
            model="gemini-2.0-flash",  # Changed to an available model
            contents=prompt,
            config={
            "temperature":0.8,
            "max_output_tokens": 1024,
            }
        )
        # grab the first candidate's output
        formatted = response.text
        return {"document_id": doc.document_id, "formatted": formatted}
    except Exception as e:
        print(f"Gemini API error: {e}")
        raise HTTPException(status_code=502, detail=f"Gemini API error: {e}")

async def consume_messages():
    """Consume messages from Kafka topic"""
    global consumer
    try:
        consumer = AIOKafkaConsumer(
            KAFKA_CONSUMER_TOPIC,
            bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
            group_id="extractor-group",
            auto_offset_reset="earliest"
        )
        await consumer.start()
        print(f"Started consuming from {KAFKA_CONSUMER_TOPIC}")
        
        async for msg in consumer:
            try:
                data = json.loads(msg.value.decode())
                print(f"Received document for extraction: {data.get('document_id', 'unknown')}")
                
                doc = DocumentContent(
                    content=data.get("content", ""),
                    document_id=data.get("document_id", "")
                )
                
                result = await process_document(doc)
                
                # Send result back to Kafka
                await producer.send_and_wait(
                    KAFKA_PRODUCER_TOPIC,
                    json.dumps(result).encode()
                )
                print(f"Processed and sent response for document: {doc.document_id}")
                
            except json.JSONDecodeError:
                print(f"Invalid JSON received: {msg.value}")
            except Exception as e:
                print(f"Error processing message: {e}")
    finally:
        if consumer:
            await consumer.stop()

@app.on_event("startup")
async def startup_event():
    """Start the Kafka consumer when the application starts"""
    global producer
    try:
        producer = AIOKafkaProducer(bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS)
        await producer.start()
        print("Kafka producer started")
        
        # Start the consumer in a background task
        asyncio.create_task(consume_messages())
    except Exception as e:
        print(f"Failed to start Kafka client: {e}")

@app.on_event("shutdown")
async def shutdown_event():
    """Stop the Kafka producer when the application shuts down"""
    global producer, consumer
    if producer:
        await producer.stop()
    if consumer:
        await consumer.stop()

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "UP"}