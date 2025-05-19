# Documents Service

## Overview
The Documents Service is a crucial component of the Document Management System (DMS) designed for healthcare environments. It handles the creation, storage, processing, and lifecycle management of medical documents. The service is built with a microservice architecture using Spring Boot, communicating with other services through Kafka event messaging.

## Key Features
- Document creation and management with various status workflows
- Medical document processing with text extraction
- Integration with AI extraction service for structured data extraction
- Role-based access control with JWT authentication
- Event-based communication with other services via Kafka topics

## Technical Stack
- **Framework**: Spring Boot
- **Database**: SQLite with JPA/Hibernate
- **Messaging**: Apache Kafka
- **Security**: JWT-based authentication
- **Build Tool**: Gradle

## Architecture

### Models
The core entity is the `Document` model which contains:
- Basic document information (title, content, type)
- Patient and doctor references
- Medical-specific fields (procedure details, operators, etc.)
- Document status tracking
- Extracted metadata storage (JSON)

### Document Lifecycle
Documents follow this workflow:
1. **PENDING**: Initial state when document is created/uploaded
2. **PROCESSING**: Document is being processed by the extraction service
3. **VALIDATED**: Document data has been validated (manually or automatically)
4. **PUBLISHED**: Document is published and available for viewing
5. **ARCHIVED**: Document has been archived (historical)
6. **REJECTED**: Document was rejected during validation

### API Endpoints
The service exposes a RESTful API:
- `GET /api/v1/documents`: Get all documents
- `GET /api/v1/documents/{id}`: Get document by ID
- `POST /api/v1/documents`: Create a new document
- `PUT /api/v1/documents/{id}`: Update a document
- `DELETE /api/v1/documents/{id}`: Delete a document
- `GET /api/v1/documents/patient/{patientId}`: Get documents by patient ID
- `GET /api/v1/documents/User/{UserId}`: Get documents by doctor/user ID
- `GET /api/v1/documents/type/{type}`: Get documents by type
- `GET /api/v1/documents/department/{department}`: Get documents by department
- `GET /api/v1/documents/status/{status}`: Get documents by status
- `PUT /api/v1/documents/{id}/status`: Update document status (admin function)

### Kafka Topics
The service publishes and consumes the following Kafka topics:
- **Publishing**:
  - `document-created`: When a new document is created
  - `document-updated`: When a document is updated
  - `document-deleted`: When a document is deleted
  - `document-uploaded`: When a document is uploaded
  - `medical-document-for-extraction`: Medical documents sent for AI extraction

- **Consuming**:
  - `document-fields-extracted`: Receives extracted fields from the extraction service
  - `document-validated`: Receives validation events
  - `document-rejected`: Receives rejection events
  - `document-published`: Receives publication events

## Security
The service implements JWT-based authentication and authorization:
- Tokens are validated against a shared secret
- Role-based access control is enforced at the endpoint level
- All API requests (except health checks) require authentication
- Cross-Origin Resource Sharing (CORS) is configured for web clients

## Configuration
Key configurations in `application.properties`:
- Server port: 8081
- SQLite database settings
- Kafka broker configuration
- JWT secret (shared with Auth service)
- CORS settings
- File upload limits

## Integrations
- **Auth Service**: For user authentication and authorization
- **Extractor Service**: For AI-powered text extraction from medical documents
- **Workflow Service**: For managing document workflows and status transitions
- **Patient Service**: Referenced by patient IDs

## Developer Notes
- The service assumes JWT tokens have user ID and roles
- All document updates trigger Kafka events
- Status transitions are controlled by business rules in the service
- Medical documents go through an AI extraction process
- The service uses a local SQLite database but can be configured for other databases

## Getting Started
1. Ensure Java 17+ and Gradle are installed
2. Configure `application.properties` with Kafka and JWT settings
3. Run with `./gradlew bootRun` or build with `./gradlew build`
4. The service will start on port 8081 (configurable)

## Testing
- JWT tokens can be obtained from the Auth service
- All API endpoints require a valid JWT token in the Authorization header