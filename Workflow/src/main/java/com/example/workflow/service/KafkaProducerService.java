package com.example.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
// UUID import java.util.UUID;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private static final String TOPIC_DOCUMENT_FIELDS_EXTRACTED = "document-fields-extracted";
    private static final String TOPIC_DOCUMENT_VALIDATED = "document-validated";
    private static final String TOPIC_DOCUMENT_REJECTED = "document-rejected";
    private static final String TOPIC_DOCUMENT_PUBLISHED = "document-published";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishExtractedFields(UUID documentId, String extractedData) {
        log.info("Publishing extracted fields for document ID: {}", documentId);
        kafkaTemplate.send(TOPIC_DOCUMENT_FIELDS_EXTRACTED, String.valueOf(documentId), 
                new DocumentFieldsEvent(documentId, extractedData));
    }
    
    public void publishDocumentValidated(UUID documentId, String validatedData) {
        log.info("Publishing document validated event for document ID: {}", documentId);
        kafkaTemplate.send(TOPIC_DOCUMENT_VALIDATED, String.valueOf(documentId),
                new DocumentStatusEvent(documentId, "VALIDATED", validatedData));
    }
    
    public void publishDocumentRejected(UUID documentId, String reason) {
        log.info("Publishing document rejected event for document ID: {}", documentId);
        kafkaTemplate.send(TOPIC_DOCUMENT_REJECTED, String.valueOf(documentId),
                new DocumentStatusEvent(documentId, "REJECTED", reason));
    }
    
    public void publishDocumentPublished(UUID documentId, String metadata) {
        log.info("Publishing document published event for document ID: {}", documentId);
        kafkaTemplate.send(TOPIC_DOCUMENT_PUBLISHED, String.valueOf(documentId),
                new DocumentStatusEvent(documentId, "PUBLISHED", metadata));
    }
    
    // Event classes
    public record DocumentFieldsEvent(UUID documentId, String extractedFields) {}
    public record DocumentStatusEvent(UUID documentId, String status, String data) {}
}