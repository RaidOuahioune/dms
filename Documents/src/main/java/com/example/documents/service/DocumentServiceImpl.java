package com.example.documents.service;

import com.example.documents.dto.DocumentDTO;
import com.example.documents.dto.DocumentRequest;
import com.example.documents.dto.ExtractionRequestDTO;
import com.example.documents.model.Document;
import com.example.documents.model.DocumentStatus;
import com.example.documents.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WordDocumentProcessingService wordDocumentProcessingService;
    private final ObjectMapper objectMapper;
    
    private static final String TOPIC_DOCUMENT_CREATED = "document-created";
    private static final String TOPIC_DOCUMENT_UPDATED = "document-updated";
    private static final String TOPIC_DOCUMENT_DELETED = "document-deleted";
    private static final String TOPIC_DOCUMENT_UPLOADED = "document-uploaded";
    // Kafka topic for sending medical documents for extraction
    private static final String TOPIC_MEDICAL_DOCUMENT_FOR_EXTRACTION = "medical-document-for-extraction";

    @Override
    public List<DocumentDTO> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentDTO getDocumentById(UUID id) {
        return documentRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with ID: " + id));
    }

    @Override
    @Transactional
    public DocumentDTO createDocument(DocumentRequest documentRequest, String createdByUserId) {
        // Determine if this is a creation or upload based on content presence
        boolean isUpload = documentRequest.getContent() != null && !documentRequest.getContent().isEmpty();
        
        Document document = Document.builder()
                .title(documentRequest.getTitle())
                .patientId(documentRequest.getPatientId())
                .diagnosis(documentRequest.getDiagnosis())
                .procedureDate(documentRequest.getProcedureDate())
                .doctorIds(documentRequest.getDoctorIds())
                .description(documentRequest.getDescription())
                // Always start with PENDING, workflow service will update status
                .status(documentRequest.getStatus() != null ? documentRequest.getStatus() : DocumentStatus.PENDING)
                .build();
                
        Document savedDocument = documentRepository.save(document);
        DocumentDTO documentDTO = mapToDTO(savedDocument);
        
        // Publish the appropriate event to Kafka
        if (isUpload) {
            log.info("Publishing document uploaded event for document ID: {}", documentDTO.getId());
            kafkaTemplate.send(TOPIC_DOCUMENT_UPLOADED, documentDTO);
        } else {
            log.info("Publishing document created event for document ID: {}", documentDTO.getId());
            kafkaTemplate.send(TOPIC_DOCUMENT_CREATED, documentDTO);
        }
        
        return documentDTO;
    }

    @Override
    @Transactional
    public DocumentDTO updateDocument(UUID id, DocumentRequest documentRequest, String updatedByUserId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with ID: " + id));
                
        document.setTitle(documentRequest.getTitle());
        document.setPatientId(documentRequest.getPatientId());
        document.setDiagnosis(documentRequest.getDiagnosis());
        document.setProcedureDate(documentRequest.getProcedureDate());
        document.setDoctorIds(documentRequest.getDoctorIds());
        document.setDescription(documentRequest.getDescription());
        
        // Only allow status updates if they're valid transitions or coming from an admin
        if (documentRequest.getStatus() != null) {
            if (isValidStatusTransition(document.getStatus(), documentRequest.getStatus())) {
                document.setStatus(documentRequest.getStatus());
                document.setStatusUpdatedAt(LocalDateTime.now());
            } else {
                log.warn("Invalid status transition attempted for document {} from {} to {}",
                    id, document.getStatus(), documentRequest.getStatus());
            }
        }
        
        Document updatedDocument = documentRepository.save(document);
        DocumentDTO documentDTO = mapToDTO(updatedDocument);
        
        // Publish the event to Kafka
        kafkaTemplate.send(TOPIC_DOCUMENT_UPDATED, documentDTO);
        
        return documentDTO;
    }

    @Override
    @Transactional
    public void deleteDocument(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with ID: " + id));
        
        documentRepository.delete(document);
        
        // Publish the event to Kafka
        kafkaTemplate.send(TOPIC_DOCUMENT_DELETED, mapToDTO(document));
    }

    @Override
    public List<DocumentDTO> getDocumentsByPatientId(String patientId) {
        return documentRepository.findByPatientId(patientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentDTO> getDocumentsByDoctorId(String doctorId) {
        // Since we removed the custom repository method, we'll filter manually
        return documentRepository.findAll().stream()
                .filter(doc -> doc.getDoctorIds() != null && doc.getDoctorIds().contains(doctorId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentDTO> getDocumentsByType(String type) {
        // Since the Document model doesn't have a type field, we'll use the diagnosis field as a substitute
        // This is a workaround to maintain compatibility with the existing API
        return documentRepository.findByDiagnosis(type).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentDTO> getDocumentsByDepartment(String department) {
        // Since Document model doesn't have a department field, we'll return all documents
        // This is a placeholder implementation
        log.warn("Department filtering not supported in current Document model");
        return getAllDocuments();
    }

    @Override
    public List<DocumentDTO> getDocumentsByStatus(DocumentStatus status) {
        return documentRepository.findByStatus(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public DocumentDTO updateDocumentStatus(UUID id, DocumentStatus status, String metadata) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with ID: " + id));
        
        if (isValidStatusTransition(document.getStatus(), status)) {
            document.setStatus(status);
            document.setStatusUpdatedAt(LocalDateTime.now());
            
            // We don't update any metadata as the Document model doesn't have that field
            
            Document updatedDocument = documentRepository.save(document);
            return mapToDTO(updatedDocument);
        } else {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s for document %s", 
                document.getStatus(), status, id));
        }
    }
    
    /**
     * Check if a status transition is valid based on workflow rules
     */
    private boolean isValidStatusTransition(DocumentStatus currentStatus, DocumentStatus newStatus) {
        // Allow same status (no change)
        if (currentStatus == newStatus) {
            return true;
        }
        
        // Define allowed transitions based on current status
        switch (currentStatus) {
            case PENDING:
                // From PENDING, can move to VALIDATED or REJECTED
                return newStatus == DocumentStatus.VALIDATED 
                    || newStatus == DocumentStatus.REJECTED;
            case VALIDATED:
                // From VALIDATED, can move to REJECTED if needed
                return newStatus == DocumentStatus.REJECTED;
            case REJECTED:
                // From REJECTED, can move back to PENDING for reconsideration
                return newStatus == DocumentStatus.PENDING;
            default:
                return false;
        }
    }
    
    private DocumentDTO mapToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .patientId(document.getPatientId())
                .diagnosis(document.getDiagnosis())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .statusUpdatedAt(document.getStatusUpdatedAt())
                .procedureDate(document.getProcedureDate())
                .doctorIds(document.getDoctorIds())
                .description(document.getDescription())
                .status(document.getStatus())
                .build();
    }

    @Override
    @Transactional
    public DocumentDTO uploadMedicalDocument(MultipartFile file, String patientId, String doctorId, String diagnosis) throws IOException {
        log.info("Processing medical document upload for patient: {}, doctor: {}, diagnosis: {}", patientId, doctorId, diagnosis);
        
        // Extract text content from document
        String content = wordDocumentProcessingService.extractText(file);
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.docx";
        
        // Create document entity
        Document document = Document.builder()
                .title("Medical Document - " + originalFilename)
                .patientId(patientId)
                .diagnosis(diagnosis)
                .description(content) // Store content in description field
                .doctorIds(doctorId) // Store doctor ID in doctorIds field
                .procedureDate(LocalDateTime.now())
                .status(DocumentStatus.PENDING)
                .build();
        
        Document savedDocument = documentRepository.save(document);
        DocumentDTO documentDTO = mapToDTO(savedDocument);
        
        // Create extraction request for AI processing
        ExtractionRequestDTO extractionRequest = ExtractionRequestDTO.builder()
                .documentId(documentDTO.getId())
                .content(documentDTO.getDescription()) // Use description as content
                .build();
        
        // Send to Kafka for AI extraction processing
        log.info("Publishing medical document to extraction topic, document ID: {}", documentDTO.getId());
        kafkaTemplate.send(TOPIC_MEDICAL_DOCUMENT_FOR_EXTRACTION, documentDTO.getId().toString(), extractionRequest);
        
        // Also send the regular document uploaded event to trigger workflow
        kafkaTemplate.send(TOPIC_DOCUMENT_UPLOADED, documentDTO);
        
        return documentDTO;
    }

    @Override
    public List<String> extractDoctorIdsFromDocument(DocumentDTO document) {
        List<String> doctorIds = new ArrayList<>();

        try {
            // Parse the comma-separated doctorIds string into a list
            if (document.getDoctorIds() != null && !document.getDoctorIds().isBlank()) {
                String[] ids = document.getDoctorIds().split(",");
                for (String id : ids) {
                    String trimmedId = id.trim();
                    if (!trimmedId.isEmpty()) {
                        doctorIds.add(trimmedId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing doctor IDs from document {}: {}", document.getId(), e.getMessage());
        }

        return doctorIds;
    }
}