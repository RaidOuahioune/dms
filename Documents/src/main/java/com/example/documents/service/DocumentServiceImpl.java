package com.example.documents.service;

import com.example.documents.dto.DocumentDTO;
import com.example.documents.dto.DocumentRequest;
import com.example.documents.model.Document;
import com.example.documents.model.DocumentStatus;
import com.example.documents.repository.DocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final KafkaTemplate<String, DocumentDTO> kafkaTemplate;
    
    private static final String TOPIC_DOCUMENT_CREATED = "document-created";
    private static final String TOPIC_DOCUMENT_UPDATED = "document-updated";
    private static final String TOPIC_DOCUMENT_DELETED = "document-deleted";
    private static final String TOPIC_DOCUMENT_UPLOADED = "document-uploaded";

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
                .content(documentRequest.getContent())
                .type(documentRequest.getType())
                .patientId(documentRequest.getPatientId())
                .doctorId(documentRequest.getDoctorId() != null ? documentRequest.getDoctorId() : createdByUserId)
                .department(documentRequest.getDepartment())
                .specialty(documentRequest.getSpecialty())
                // Always start with PENDING, workflow service will update status
                .status(DocumentStatus.PENDING)
                .statusUpdatedAt(LocalDateTime.now())
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
        document.setContent(documentRequest.getContent());
        document.setType(documentRequest.getType());
        document.setPatientId(documentRequest.getPatientId());
        document.setDoctorId(documentRequest.getDoctorId());
        document.setDepartment(documentRequest.getDepartment());
        document.setSpecialty(documentRequest.getSpecialty());
        
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
        return documentRepository.findByDoctorId(doctorId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentDTO> getDocumentsByType(String type) {
        return documentRepository.findByType(type).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentDTO> getDocumentsByDepartment(String department) {
        return documentRepository.findByDepartment(department).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
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
            
            // Update metadata if provided
            if (metadata != null && !metadata.isEmpty()) {
                document.setExtractedMetadata(metadata);
            }
            
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
        if (currentStatus == newStatus) {
            return true; // No change
        }
        
        // Define allowed transitions
        switch (currentStatus) {
            case PENDING:
                return newStatus == DocumentStatus.PROCESSING 
                    || newStatus == DocumentStatus.REJECTED
                    || newStatus == DocumentStatus.VALIDATED; // Direct validation for simple docs
                
            case PROCESSING:
                return newStatus == DocumentStatus.VALIDATED 
                    || newStatus == DocumentStatus.REJECTED;
                
            case VALIDATED:
                return newStatus == DocumentStatus.PUBLISHED 
                    || newStatus == DocumentStatus.ARCHIVED;
                
            case PUBLISHED:
                return newStatus == DocumentStatus.ARCHIVED;
                
            case REJECTED:
                return newStatus == DocumentStatus.PENDING; // Allow reprocessing
                
            case DRAFT:
                return true; // Drafts can transition to any state
                
            case ARCHIVED:
                // Archived is a terminal state
                return false;
                
            default:
                return false;
        }
    }
    
    private DocumentDTO mapToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .content(document.getContent())
                .type(document.getType())
                .patientId(document.getPatientId())
                .doctorId(document.getDoctorId())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .department(document.getDepartment())
                .specialty(document.getSpecialty())
                .status(document.getStatus())
                .extractedMetadata(document.getExtractedMetadata())
                .statusUpdatedAt(document.getStatusUpdatedAt())
                .build();
    }
}