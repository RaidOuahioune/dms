package com.example.workflow.listener;

import com.example.workflow.dto.DocumentEventDTO;
import com.example.workflow.model.WorkflowType;
import com.example.workflow.service.KafkaProducerService;
import com.example.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEventListener {

    private static final String TOPIC_DOCUMENT_CREATED = "document-created";
    private static final String TOPIC_DOCUMENT_UPDATED = "document-updated";
    private static final String TOPIC_DOCUMENT_DELETED = "document-deleted";
    private static final String TOPIC_DOCUMENT_UPLOADED = "document-uploaded";
    
    private final WorkflowService workflowService;
    private final KafkaProducerService kafkaProducerService;

    @KafkaListener(topics = TOPIC_DOCUMENT_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    public void handleDocumentCreated(DocumentEventDTO documentEvent) {
        log.info("Received document created event for document ID: {}", documentEvent.getId());
        
        // For document creation, we create a workflow that immediately marks it as submitted
        var workflow = workflowService.createWorkflow(documentEvent.getId(), WorkflowType.DOCUMENT_CREATION);
        log.info("Created workflow for document ID: {} with DOCUMENT_CREATION type", documentEvent.getId());
        
        // For created documents, they are automatically published and valid
        kafkaProducerService.publishDocumentValidated(documentEvent.getId(), "{}");
        log.info("Published document validated event for document ID: {}", documentEvent.getId());
    }

    @KafkaListener(topics = TOPIC_DOCUMENT_UPLOADED, groupId = "${spring.kafka.consumer.group-id}")
    public void handleDocumentUploaded(DocumentEventDTO documentEvent) {
        log.info("Received document uploaded event for document ID: {}", documentEvent.getId());
        
        // For uploaded documents, create a workflow that starts the field extraction process
        var workflow = workflowService.createWorkflow(documentEvent.getId(), WorkflowType.DOCUMENT_UPLOAD);
        log.info("Created workflow for document ID: {} with DOCUMENT_UPLOAD type. Starting AI field extraction...", documentEvent.getId());
        
        // Here you would integrate with an AI service to extract fields
        // This is a placeholder for the AI field extraction logic
        simulateAiFieldExtraction(workflow.getId(), documentEvent);
    }
    
    // This is a placeholder method for simulating AI field extraction
    // In a real implementation, this would call an actual AI service
    private void simulateAiFieldExtraction(Long workflowId, DocumentEventDTO document) {
        try {
            // Simulate processing delay
            Thread.sleep(2000);
            
            // Create a simple JSON structure with extracted fields
            String extractedData = String.format("""
                {
                  "title": "%s",
                  "extractedFields": {
                    "documentType": "Medical Record",
                    "patientName": "John Doe",
                    "documentDate": "2025-04-23"
                  }
                }
                """, document.getTitle());
            
            // Send extracted data directly to Documents service (no longer storing in Workflow)
            kafkaProducerService.publishExtractedFields(document.getId(), extractedData);
            
            // Update the workflow status only, not storing the extracted data
            workflowService.processNextStep(document.getId(), null);
            log.info("Field extraction completed for document ID: {} and published to Documents service", document.getId());
        } catch (Exception e) {
            log.error("Error during AI field extraction for document ID: {}", document.getId(), e);
        }
    }
}