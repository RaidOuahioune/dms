# Workflow Microservice

## Overview
The Workflow Microservice is an integral component of the Document Management System (DMS) that manages the lifecycle and processing states of documents. It works closely with the Documents Microservice through Kafka events to implement automated and human-assisted document processing workflows.

## Architecture

### Core Concepts

- **Workflow Instance**: Represents a specific document's journey through the system
- **Workflow Type**: Categorizes workflows based on their origin (DOCUMENT_CREATION or DOCUMENT_UPLOAD)
- **Workflow Status**: Tracks the document's current position in the workflow pipeline

### Status Progression

1. **For Created Documents**:
   - SUBMITTED → PUBLISHED (automatic)

2. **For Uploaded Documents**:
   - FIELD_EXTRACTION_PENDING → VALIDATION_PENDING → VALIDATED → PUBLISHED

### Workflow Statuses

| Status | Description |
|--------|-------------|
| SUBMITTED | Document has been submitted to the system |
| FIELD_EXTRACTION_PENDING | Document is waiting for AI field extraction |
| VALIDATION_PENDING | Document fields have been extracted and await human validation |
| VALIDATED | Document fields have been validated by a doctor/user |
| PUBLISHED | Document has completed its workflow and is published |
| REJECTED | Document has been rejected during the workflow |

## Event-Driven Communication

### Inbound Events (Consumed Topics)
The microservice listens to Kafka topics for document events:

| Topic | Description |
|-------|-------------|
| document-created | Triggered when a document is created (starts DOCUMENT_CREATION workflow) |
| document-uploaded | Triggered when a document is uploaded (starts DOCUMENT_UPLOAD workflow) |
| document-updated | Notifies of document updates |
| document-deleted | Notifies of document deletions |

### Outbound Events (Published Topics)
The microservice publishes Kafka events to notify the Documents service:

| Topic | Description |
|-------|-------------|
| document-fields-extracted | Publishes extracted fields from document to update metadata |
| document-validated | Notifies that a document has been validated by doctor/user |
| document-rejected | Notifies that a document has been rejected in the workflow |

## API Endpoints

### Workflow Management

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/workflows/document/{documentId}` | GET | Retrieve workflow information for a specific document |
| `/api/workflows/status/{status}` | GET | List all workflows with a specific status |
| `/api/workflows/{workflowId}/validate` | PUT | Mark document fields as validated |
| `/api/workflows/{workflowId}/publish` | PUT | Publish a document |
| `/api/workflows/{workflowId}/reject` | PUT | Reject a document |
| `/api/workflows/document/{documentId}/extracted-data` | POST | Send extracted field data to Documents service |

### Stateful Workflow Processing

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/workflows/document/{documentId}/next` | POST | Process the next step in the workflow automatically |
| `/api/workflows/document/{documentId}/workflow-info` | GET | Get information about current workflow state and next action |

### Health Check

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/health` | GET | Check service health status |

## Stateful API Benefits

The `/next` endpoint provides a stateful workflow experience:

1. **Simplified Client Logic**: Clients need only call "next" without knowing the internal workflow logic
2. **Guided Experience**: The workflow-info endpoint guides users through what's needed next
3. **Consistent State Management**: All rules are centralized server-side
4. **Predictable Transitions**: State machine ensures clear progression through the workflow

## Technology Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 21
- **Database**: SQLite (via JPA/Hibernate)
- **Messaging**: Apache Kafka
- **Container**: Docker
- **API**: RESTful with JSON

## How AI Field Extraction Works

When documents are uploaded, the workflow service:

1. Creates a new workflow with FIELD_EXTRACTION_PENDING status
2. Initiates AI field extraction (currently simulated)
3. Extracts document fields into a structured JSON format
4. Publishes extracted fields directly to Documents service via Kafka
5. Transitions the document to VALIDATION_PENDING status
6. Awaits human validation before proceeding

## Document Status Notifications

The workflow service notifies the Documents service about document status changes:

1. **Field Extraction**: Sends structured extracted data to update document metadata
2. **Validation**: Notifies when a doctor/user validates the document fields
3. **Rejection**: Notifies when a document is rejected during the workflow
4. **Publication**: Confirms when a document completes the workflow and is published

## Data Responsibility Separation

The Workflow service follows a strict separation of concerns:

1. **Workflow State Management**: The Workflow service only stores and manages workflow state information
2. **Document Data Management**: The Documents service is the single source of truth for all document data, including AI-extracted fields
3. **Pass-through Processing**: Extracted data is never stored in the Workflow service - it is passed directly to the Documents service

## Sample Workflow Use Case

### Document Upload Workflow

1. User uploads a document via Documents service
2. Documents service sends a message to `document-uploaded` Kafka topic
3. Workflow service receives the event and creates a new workflow
4. AI automatically extracts fields from the document
5. Extracted fields are sent directly to Documents service via `document-fields-extracted` topic
6. Doctor/user validates the extracted fields via the API
7. Validation result is sent to Documents service via `document-validated` topic
8. Document is published and becomes available

### Using the Stateful API

```
# Step 1: Check current workflow state
GET /api/workflows/document/123/workflow-info
Response: {
  "documentId": 123,
  "currentStatus": "VALIDATION_PENDING",
  "nextActionDescription": "Validate extracted fields",
  "isComplete": false
}

# Step 2: Process next step (validation)
POST /api/workflows/document/123/next
Body: "{\"validatedFields\":{\"patientName\":\"John Doe\",\"documentDate\":\"2025-04-23\"}}"
Response: {
  "id": 456,
  "documentId": 123,
  "workflowType": "DOCUMENT_UPLOAD",
  "currentStatus": "VALIDATED",
  "createdAt": "2025-04-23T10:15:30",
  "updatedAt": "2025-04-23T10:20:45"
}

# Step 3: Process final step (publishing)
POST /api/workflows/document/123/next
Response: {
  "id": 456,
  "documentId": 123, 
  "workflowType": "DOCUMENT_UPLOAD",
  "currentStatus": "PUBLISHED",
  "createdAt": "2025-04-23T10:15:30", 
  "updatedAt": "2025-04-23T10:21:15"
}
```

## Integration with Documents Service

The Workflow service integrates with the Documents service through Kafka events:

```java
// Example of publishing extracted fields (without storing locally)
kafkaProducerService.publishExtractedFields(documentId, extractedData);

// Example of publishing validation
kafkaProducerService.publishDocumentValidated(documentId, validatedData);

// Example of publishing rejection
kafkaProducerService.publishDocumentRejected(documentId, rejectionReason);
```

## Configuration

The service is configured through `application.properties`:

```properties
server.port=8083
spring.application.name=workflow-service

# SQLite Database Configuration
spring.datasource.url=jdbc:sqlite:${SQLITE_DB_PATH:./data/workflow.db}
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update

# Kafka Configuration
spring.kafka.bootstrap-servers=kafka:9092
spring.kafka.consumer.group-id=workflow-service-group
spring.kafka.consumer.auto-offset-reset=earliest
```

## Docker Integration

The workflow service runs as a containerized microservice:

```yaml
workflow-service:
  build:
    context: ./Workflow
    dockerfile: Dockerfile
  container_name: workflow-service
  ports:
    - "8083:8083"
  environment:
    - SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/workflow.db
    - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    - SPRING_PROFILES_ACTIVE=docker
  volumes:
    - workflow-data:/app/data
  depends_on:
    - kafka
    - documents-service
  networks:
    - microservices-net
```

## Getting Started

1. Start the complete system with Docker Compose:
   ```bash
   docker-compose up --build
   ```

2. Access the workflow API through the gateway:
   ```
   http://localhost:4000/api/workflows/...
   ```

3. Check service health:
   ```
   http://localhost:4000/api/health
   ```

## Development Notes

- The workflow database stores minimal information and references documents by ID
- Document details are stored and managed by the Documents microservice
- For local development, run with the `local` profile to use a local SQLite database
- The database will be created in the `/app/data/` directory in Docker or in `./data/` for local development

## Future Enhancements

- Implement asynchronous AI processing for field extraction
- Add support for more complex workflow patterns and branching
- Implement retries and error handling for failed workflows
- Integrate notification service for status updates
- Add audit logging for all workflow transitions