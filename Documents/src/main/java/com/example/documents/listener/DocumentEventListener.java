package com.example.documents.listener;

import com.example.documents.dto.workflow.WorkflowEventDTO;
import com.example.documents.dto.workflow.DocumentStatusEvent;
import com.example.documents.model.Document;
import com.example.documents.model.DocumentStatus;
import com.example.documents.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEventListener {

    private static final String TOPIC_DOCUMENT_FIELDS_EXTRACTED = "document-fields-extracted";
    private static final String TOPIC_DOCUMENT_VALIDATED = "document-validated";
    private static final String TOPIC_DOCUMENT_REJECTED = "document-rejected";
    
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Helper method to extract document ID from a Kafka consumer record
     * 
     * @param eventObj Kafka consumer record
     * @return UUID of the document or null if it couldn't be extracted
     */
    private UUID extractDocumentId(ConsumerRecord<String, String> eventObj) {
        try {
            // First try to get the ID from the key
            if (eventObj.key() != null) {
                try {
                    // Try to parse directly as UUID (for direct UUID strings)
                    UUID id = UUID.fromString(eventObj.key());
                    log.info("Extracted document ID as UUID from key: {}", id);
                    return id;
                } catch (IllegalArgumentException e) {
                    log.error("Key is not a valid UUID: {}", eventObj.key());
                }
            }
            
            // If key is null or invalid, try to parse from value
            try {
                // Try to parse as DocumentStatusEvent
                DocumentStatusEvent event = objectMapper.readValue(eventObj.value(), DocumentStatusEvent.class);
                
                UUID documentId = event.getDocumentId();
                if (documentId != null) {
                    log.info("Processed as DocumentStatusEvent with UUID: {}", documentId);
                    return documentId;
                } else {
                    log.error("DocumentStatusEvent has null documentId");
                }
                
            } catch (Exception e) {
                try {
                    // Try to parse as WorkflowEventDTO
                    WorkflowEventDTO event = objectMapper.readValue(eventObj.value(), WorkflowEventDTO.class);
                    try {
                        UUID documentId = event.getDocumentId();
                        
                        log.info("Processed as WorkflowEventDTO with ID: {} (converted to UUID: {})", documentId);
                        return documentId;
                    } catch (Exception ex) {
                        log.error("Failed to convert WorkflowEventDTO ID to UUID", ex);
                    }
                } catch (Exception ex) {
                    log.error("Could not parse document ID from event value", ex);
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error extracting document ID", e);
            return null;
        }
    }
    
    /**
     * Handle document field extraction events
     */
    @KafkaListener(topics = TOPIC_DOCUMENT_FIELDS_EXTRACTED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleDocumentFieldsExtracted(ConsumerRecord<String, String> eventObj) {
        log.info("Received document fields extraction event type: {}", eventObj.getClass().getName());
        
        try {
            final UUID documentId = extractDocumentId(eventObj);
            if (documentId == null) {
                log.error("Could not extract document ID from event");
                return;
            }
            
            final String extractedData = eventObj.value();
            
            documentRepository.findById(documentId).ifPresentOrElse(
                document -> {
                    document.setStatus(DocumentStatus.PENDING); // Use PENDING instead of PROCESSING
                    
                    // Store extracted data in description field since Document model doesn't have extractedMetadata field
                    String currentDescription = document.getDescription() != null ? document.getDescription() : "";
                    String newDescription = currentDescription + "\n\n--- EXTRACTED DATA ---\n" + extractedData;
                    document.setDescription(newDescription);
                    
                    document.setStatusUpdatedAt(LocalDateTime.now());
                    documentRepository.save(document);
                    log.info("Updated document {} with extracted fields", documentId);
                },
                () -> log.error("Document with ID {} not found", documentId)
            );
        } catch (Exception e) {
            log.error("Error processing document fields extraction event", e);
        }
    }
    
    /**
     * Handle document validation events
     */
    @KafkaListener(
        topics = TOPIC_DOCUMENT_VALIDATED, 
        groupId = "${spring.kafka.consumer.group-id}", 
        containerFactory = "documentStatusKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDocumentValidated(DocumentStatusEvent event) {
        log.info("Received document validated event for document ID: {}", event.getDocumentId());
        
        try {
            // Use the UUID document ID directly
            final UUID documentId = event.getDocumentId();
            log.info("Processing document with UUID: {}", documentId);
            
            // Update document status in repository
            documentRepository.findById(documentId).ifPresentOrElse(
                document -> {
                    document.setStatus(DocumentStatus.VALIDATED);
                    document.setStatusUpdatedAt(LocalDateTime.now());
                    
                    documentRepository.save(document);
                    log.info("Updated document {} status to VALIDATED", documentId);
                },
                () -> log.error("Document with ID {} not found", documentId)
            );
        } catch (Exception e) {
            log.error("Error processing document validated event", e);
        }
    }
    
    /**
     * Handle document rejection events
     */
    @KafkaListener(
        topics = TOPIC_DOCUMENT_REJECTED, 
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "documentStatusKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDocumentRejected(DocumentStatusEvent event) {
        log.info("Received document rejected event for document ID: {}", event.getDocumentId());
        
        try {
            // Use the UUID document ID directly
            final UUID documentId = event.getDocumentId();
            log.info("Processing document with UUID: {}", documentId);
            
            documentRepository.findById(documentId).ifPresentOrElse(
                document -> {
                    document.setStatus(DocumentStatus.REJECTED);
                    document.setStatusUpdatedAt(LocalDateTime.now());
                    documentRepository.save(document);
                    log.info("Updated document {} status to REJECTED", documentId);
                },
                () -> log.error("Document with ID {} not found", documentId)
            );
        } catch (Exception e) {
            log.error("Error processing document rejected event", e);
        }
    }
    
    /**
     * Additional listener for document publication event from workflow
     */
    @KafkaListener(
        topics = "document-published", 
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "documentStatusKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDocumentPublished(DocumentStatusEvent event) {
        log.info("Received document published event for document ID: {}", event.getDocumentId());
        
        try {
            // Use the UUID document ID directly
            final UUID documentId = event.getDocumentId();
            log.info("Processing document with UUID: {}", documentId);
            
            documentRepository.findById(documentId).ifPresentOrElse(
                document -> {
                    document.setStatus(DocumentStatus.VALIDATED); // Use VALIDATED instead of PUBLISHED
                    document.setStatusUpdatedAt(LocalDateTime.now());
                    documentRepository.save(document);
                    log.info("Updated document {} status to VALIDATED (from publish event)", documentId);
                },
                () -> log.error("Document with ID {} (UUID: {}) not found", documentId)
            );
        } catch (Exception e) {
            log.error("Error processing document published event", e);
        }
    }
}