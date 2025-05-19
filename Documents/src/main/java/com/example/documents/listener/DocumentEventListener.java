package com.example.documents.listener;

import com.example.documents.dto.workflow.WorkflowEventDTO;
import com.example.documents.model.Document;
import com.example.documents.model.DocumentStatus;
import com.example.documents.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
     * Handle document field extraction events
     */
    @KafkaListener(topics = TOPIC_DOCUMENT_FIELDS_EXTRACTED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleDocumentFieldsExtracted(WorkflowEventDTO event) {
        log.info("Received document fields extraction event for document ID: {}", event.getDocumentId());
        
        documentRepository.findById(event.getDocumentId()).ifPresentOrElse(
            document -> {
                document.setStatus(DocumentStatus.PENDING); // Use PENDING instead of PROCESSING
                
                // Store extracted data in description field since Document model doesn't have extractedMetadata field
                String currentDescription = document.getDescription() != null ? document.getDescription() : "";
                String newDescription = currentDescription + "\n\n--- EXTRACTED DATA ---\n" + event.getData();
                document.setDescription(newDescription);
                
                document.setStatusUpdatedAt(LocalDateTime.now());
                documentRepository.save(document);
                log.info("Updated document {} with extracted fields", event.getDocumentId());
            },
            () -> log.error("Document with ID {} not found", event.getDocumentId())
        );
    }
    
    /**
     * Handle document validation events
     */
    @KafkaListener(topics = TOPIC_DOCUMENT_VALIDATED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleDocumentValidated(WorkflowEventDTO event) {
        log.info("Received document validated event for document ID: {}", event.getDocumentId());
        
        documentRepository.findById(event.getDocumentId()).ifPresentOrElse(
            document -> {
                document.setStatus(DocumentStatus.VALIDATED);
                document.setStatusUpdatedAt(LocalDateTime.now());
                
                // If there's additional validated data, update the document
                if (event.getData() != null && !event.getData().isEmpty() 
                        && !event.getData().equals("{}")) {
                    // Store validated data in description field
                    String currentDescription = document.getDescription() != null ? document.getDescription() : "";
                    String newDescription = currentDescription + "\n\n--- VALIDATED DATA ---\n" + event.getData();
                    document.setDescription(newDescription);
                }
                
                documentRepository.save(document);
                log.info("Updated document {} status to VALIDATED", event.getDocumentId());
            },
            () -> log.error("Document with ID {} not found", event.getDocumentId())
        );
    }
    
    /**
     * Handle document rejection events
     */
    @KafkaListener(topics = TOPIC_DOCUMENT_REJECTED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleDocumentRejected(WorkflowEventDTO event) {
        log.info("Received document rejected event for document ID: {}", event.getDocumentId());
        
        documentRepository.findById(event.getDocumentId()).ifPresentOrElse(
            document -> {
                document.setStatus(DocumentStatus.REJECTED);
                document.setStatusUpdatedAt(LocalDateTime.now());
                documentRepository.save(document);
                log.info("Updated document {} status to REJECTED", event.getDocumentId());
            },
            () -> log.error("Document with ID {} not found", event.getDocumentId())
        );
    }
    
    /**
     * Additional listener for document publication event from workflow
     */
    @KafkaListener(topics = "document-published", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleDocumentPublished(WorkflowEventDTO event) {
        log.info("Received document published event for document ID: {}", event.getDocumentId());
        
        documentRepository.findById(event.getDocumentId()).ifPresentOrElse(
            document -> {
                document.setStatus(DocumentStatus.VALIDATED); // Use VALIDATED instead of PUBLISHED
                document.setStatusUpdatedAt(LocalDateTime.now());
                documentRepository.save(document);
                log.info("Updated document {} status to VALIDATED (from publish event)", event.getDocumentId());
            },
            () -> log.error("Document with ID {} not found", event.getDocumentId())
        );
    }
}