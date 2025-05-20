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
# Define both regular and medical document extraction topics
KAFKA_CONSUMER_TOPIC = "extraction"
KAFKA_MEDICAL_CONSUMER_TOPIC = "medical-document-for-extraction"
KAFKA_PRODUCER_TOPIC = "extraction_response"

# Kafka consumer and producer
producer = None
consumer = None
medical_consumer = None

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

@app.post("/extract-medical")
async def extract_medical(doc: DocumentContent):
    """HTTP endpoint to manually trigger medical document extraction"""
    try:
        result = await process_medical_document(doc)
        return result
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Medical extraction error: {str(e)}")

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

async def process_medical_document(doc: DocumentContent):
    """Process medical document content through Gemini API with specific medical extraction"""
    prompt = (
        "Extract the following fields from this medical document and return as JSON:\n\n"
        "1. date: The procedure date in ISO format (YYYY-MM-DD)\n"
        "2. patientId: Extract patient name/ID\n"
        "3. operators: Array of doctor names who operated the procedure\n"
        "4. procedure_time: Extract the procedure time in minutes as integer\n"
        "5. conclusion: Extract the medical conclusion section\n"
        "6. procedure_details: Format any tables or structured data as a JSON object\n\n"
        f"Document content:\n{doc.content}\n\n"
        "Return ONLY valid JSON with this exact structure - no explanations:\n"
        "{\n"
        '  "date": "YYYY-MM-DD",\n'
        '  "patientId": "patient name or ID",\n'
        '  "operators": ["doctor1", "doctor2", ...],\n'
        '  "procedure_time": number_in_minutes,\n'
        '  "conclusion": "text of conclusion",\n'
        '  "procedure_details": { structured data as needed }\n'
        "}"
    )
    try:
        response = client.models.generate_content(
            model="gemini-2.0-flash",
            contents=prompt,
            config={
                "temperature": 0.2,  # Lower temperature for more deterministic output
                "max_output_tokens": 1024,
            }
        )
        # grab the first candidate's output
        formatted = response.text
        
        # Try to parse JSON to validate it
        try:
            parsed_json = json.loads(formatted)
            print(f"Successfully parsed medical document data for {doc.document_id}")
        except json.JSONDecodeError:
            print(f"Invalid JSON returned: {formatted}")
            # Try to extract just the JSON part if there are surrounding explanations
            import re
            json_match = re.search(r'({[\s\S]*})', formatted)
            if json_match:
                formatted = json_match.group(1)
                # Validate again
                try:
                    parsed_json = json.loads(formatted)
                except json.JSONDecodeError:
                    print("Still invalid JSON after extraction attempt")
                    formatted = "{}"
            else:
                formatted = "{}"
        
        return {"document_id": doc.document_id, "formatted": formatted}
    except Exception as e:
        print(f"Gemini API error: {e}")
        raise HTTPException(status_code=502, detail=f"Gemini API error: {e}")

async def consume_messages():
    """Consume messages from standard extraction Kafka topic"""
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

async def consume_medical_documents():
    """Consume messages from medical document extraction Kafka topic"""
    global medical_consumer
    try:
        medical_consumer = AIOKafkaConsumer(
            KAFKA_MEDICAL_CONSUMER_TOPIC,
            bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
            group_id="medical-extractor-group",
            auto_offset_reset="earliest"
        )
        await medical_consumer.start()
        print(f"Started consuming from {KAFKA_MEDICAL_CONSUMER_TOPIC}")
        
        async for msg in medical_consumer:
            try:
                data = json.loads(msg.value.decode())
                print(f"Received medical document for extraction: {data.get('documentId', 'unknown')}")
                
                doc = DocumentContent(
                    content=data.get("content", ""),
                    document_id=str(data.get("documentId", ""))
                )
                
                result = await process_medical_document(doc)
                
                # Send result back to Kafka
                await producer.send_and_wait(
                    KAFKA_PRODUCER_TOPIC,
                    json.dumps(result).encode()
                )
                print(f"Processed and sent response for medical document: {doc.document_id}")
                
            except json.JSONDecodeError:
                print(f"Invalid JSON received: {msg.value}")
            except Exception as e:
                print(f"Error processing medical document message: {e}")
    finally:
        if medical_consumer:
            await medical_consumer.stop()

@app.on_event("startup")
async def startup_event():
    """Start the Kafka consumers when the application starts"""
    global producer, consumer, medical_consumer
    try:
        producer = AIOKafkaProducer(bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS)
        await producer.start()
        print("Kafka producer started")
        
        # Start the standard consumer in a background task
        asyncio.create_task(consume_messages())
        
        # Start the medical document consumer in a background task
        asyncio.create_task(consume_medical_documents())
    except Exception as e:
        print(f"Failed to start Kafka client: {e}")

@app.on_event("shutdown")
async def shutdown_event():
    """Stop the Kafka producer and consumers when the application shuts down"""
    global producer, consumer, medical_consumer
    if producer:
        await producer.stop()
    if consumer:
        await consumer.stop()
    if medical_consumer:
        await medical_consumer.stop()

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "UP"}