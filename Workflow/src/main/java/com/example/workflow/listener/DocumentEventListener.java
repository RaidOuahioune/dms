package com.example.workflow.listener;

import com.example.documents.dto.DocumentDTO;
import com.example.workflow.model.WorkflowType;
import com.example.workflow.service.KafkaProducerService;
import com.example.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

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
    public void handleDocumentCreated(DocumentDTO document) {
        log.info("Received document created event for document ID: {}", document.getId());
        
        // Convert UUID to UUID for our internal services
        UUID documentIdUUID= document.getId();
        
        // For document creation, we create a workflow that immediately marks it as submitted
        var workflow = workflowService.createWorkflow(documentIdUUID, WorkflowType.DOCUMENT_CREATION);
        log.info("Created workflow for document ID: {} with DOCUMENT_CREATION type", document.getId());
        
        // For created documents, they are automatically published and valid
        kafkaProducerService.publishDocumentValidated(documentIdUUID, "{}");
        log.info("Published document validated event for document ID: {}", document.getId());
    }

    @KafkaListener(topics = TOPIC_DOCUMENT_UPLOADED, groupId = "${spring.kafka.consumer.group-id}")
    public void handleDocumentUploaded(DocumentDTO document) {
        log.info("Received document uploaded event for document ID: {}", document.getId());
        
        // Convert UUID to UUID for our internal services
        UUID documentIdUUID = document.getId();
        
        // For uploaded documents, create a workflow that starts the field extraction process
        var workflow = workflowService.createWorkflow(documentIdUUID, WorkflowType.DOCUMENT_UPLOAD);
        log.info("Created workflow for document ID: {} with DOCUMENT_UPLOAD type. Starting AI field extraction...", document.getId());
        
        // Here you would integrate with an AI service to extract fields
        // This is a placeholder for the AI field extraction logic
        simulateAiFieldExtraction(workflow.getId(), document);
    }
    
    // This is a placeholder method for simulating AI field extraction
    // In a real implementation, this would call an actual AI service
    private void simulateAiFieldExtraction(UUID workflowId, DocumentDTO document) {
        try {
            // Simulate processing delay
            Thread.sleep(2000);
            
            // Convert UUID to UUID for our internal services
            UUID documentIdUUID = document.getId();
            
            // Create a simple JSON structure with extracted fields
            String extractedData = String.format("""
                {
                  "title": "%s",
                  "extractedFields": {
                    "documentType": "Medical Record",
                    "patientName": "%s",
                    "documentDate": "2025-04-23",
                    "diagnosis": "%s"
                  }
                }
                """, document.getTitle(), document.getPatientId(), document.getDiagnosis());
            
            // Send extracted data directly to Documents service (no UUIDer storing in Workflow)
            kafkaProducerService.publishExtractedFields(documentIdUUID, extractedData);
            
            // Update the workflow status only, not storing the extracted data
            workflowService.processNextStep(documentIdUUID, null);
            log.info("Field extraction completed for document ID: {} and published to Documents service", document.getId());
        } catch (Exception e) {
            log.error("Error during AI field extraction for document ID: {}", document.getId(), e);
        }
    }
}