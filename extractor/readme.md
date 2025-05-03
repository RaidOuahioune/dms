# Extractor Microservice

This is a document extraction service that processes content using the Google Gemini AI model to convert unstructured document content into structured JSON formats.

## Overview

The Extractor service is a FastAPI-based microservice that:

1. Provides an HTTP API endpoint for document extraction
2. Integrates with Kafka for asynchronous processing
3. Uses Google's Gemini API to transform document content into structured JSON

## Integration Points

### HTTP API

- **Endpoint**: `/extract` (POST)
- **Input**: JSON with document content and ID
- **Output**: JSON with document ID and formatted content

### Kafka Integration

The service connects to Kafka as both consumer and producer:

- **Consumes from**: `extraction` topic
  - Expected message format: `{"content": "...", "document_id": "..."}`
- **Publishes to**: `extraction_response` topic
  - Published format: `{"document_id": "...", "formatted": "..."}`

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `GENAI_API_KEY` | Google Gemini API key | None (required) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address | `kafka:9092` |

## Integration with Other Services

### Documents Service Integration

1. The Documents Service publishes document content to the `extraction` Kafka topic
2. Extractor processes the content and publishes results back to `extraction_response`
3. Documents Service consumes the processed data from `extraction_response`

### API Gateway Integration

The service is exposed through the Nginx API Gateway:
- External endpoint: `POST /api/extract`
- Health check: `GET /api/extractor/health`

## Docker Deployment

The service runs as a Docker container and is defined in `docker-compose.yml`.
Required connections:
- Kafka broker
- Internet access for Google Gemini API calls

## Testing the Integration

1. **Send a test message to Kafka**:
   ```bash
   # Using kafkacat
   echo '{"content": "Invoice #12345...", "document_id": "test-123"}' | kafkacat -b localhost:9092 -t extraction -P