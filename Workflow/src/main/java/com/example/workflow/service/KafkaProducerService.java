package com.example.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private static final String TOPIC_DOCUMENT_FIELDS_EXTRACTED = "document-fields-extracted";
    private static final String TOPIC_DOCUMENT_VALIDATED = "document-validated";
    private static final String TOPIC_DOCUMENT_REJECTED = "document-rejected";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishExtractedFields(Long documentId, String extractedData) {
        log.info("Publishing extracted fields for document ID: {}", documentId);
        kafkaTemplate.send(TOPIC_DOCUMENT_FIELDS_EXTRACTED, String.valueOf(documentId), 
                new DocumentFieldsEvent(documentId, extractedData));
    }
    
    public void publishDocumentValidated(Long documentId, String validatedData) {
        log.info("Publishing document validated event for document ID: {}", documentId);
        kafkaTemplate.send(TOPIC_DOCUMENT_VALIDATED, String.valueOf(documentId),
                new DocumentStatusEvent(documentId, "VALIDATED", validatedData));
    }
    
    public void publishDocumentRejected(Long documentId, String reason) {
        log.info("Publishing document rejected event for document ID: {}", documentId);
        kafkaTemplate.send(TOPIC_DOCUMENT_REJECTED, String.valueOf(documentId),
                new DocumentStatusEvent(documentId, "REJECTED", reason));
    }
    
    // Event classes
    public record DocumentFieldsEvent(Long documentId, String extractedFields) {}
    public record DocumentStatusEvent(Long documentId, String status, String data) {}
}