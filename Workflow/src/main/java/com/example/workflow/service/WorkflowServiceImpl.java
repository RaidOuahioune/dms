package com.example.workflow.service;

import com.example.workflow.dto.WorkflowInstanceDTO;
import com.example.workflow.model.WorkflowInstance;
import com.example.workflow.model.WorkflowStatus;
import com.example.workflow.model.WorkflowType;
import com.example.workflow.repository.WorkflowRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final KafkaProducerService kafkaProducerService;

    @Override
    @Transactional
    public WorkflowInstance createWorkflow(UUID documentId, WorkflowType workflowType) {
        WorkflowInstance workflow = new WorkflowInstance();
        workflow.setDocumentId(documentId);
        workflow.setWorkflowType(workflowType);
        
        // Set the initial status based on workflow type
        if (workflowType == WorkflowType.DOCUMENT_CREATION) {
            workflow.setCurrentStatus(WorkflowStatus.SUBMITTED);
        } else if (workflowType == WorkflowType.DOCUMENT_UPLOAD) {
            workflow.setCurrentStatus(WorkflowStatus.FIELD_EXTRACTION_PENDING);
        }
        
        return workflowRepository.save(workflow);
    }

    @Override
    @Transactional
    public WorkflowInstance updateWorkflowStatus(UUID workflowId, WorkflowStatus newStatus) {
        WorkflowInstance workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new EntityNotFoundException("Workflow with ID " + workflowId + " not found"));
        
        WorkflowStatus previousStatus = workflow.getCurrentStatus();
        workflow.setCurrentStatus(newStatus);
        
        // Notify Documents service about status changes
        if (newStatus == WorkflowStatus.VALIDATED) {
            kafkaProducerService.publishDocumentValidated(
                workflow.getDocumentId(), 
                "{}" // Empty data as we no UUIDer store extraction data
            );
            log.info("Published document validated event for document ID: {}", workflow.getDocumentId());
        } else if (newStatus == WorkflowStatus.REJECTED) {
            kafkaProducerService.publishDocumentRejected(
                workflow.getDocumentId(), 
                "Document rejected during workflow validation step"
            );
            log.info("Published document rejected event for document ID: {}", workflow.getDocumentId());
        } else if (newStatus == WorkflowStatus.PUBLISHED) {
            kafkaProducerService.publishDocumentPublished(
                workflow.getDocumentId(),
                "{}" // Empty metadata
            );
            log.info("Published document published event for document ID: {}", workflow.getDocumentId());
        }
        
        return workflowRepository.save(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowInstance> getWorkflowByDocumentId(UUID documentId) {
        return workflowRepository.findByDocumentId(documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstance> getWorkflowsByStatus(WorkflowStatus status) {
        return workflowRepository.findByCurrentStatus(status);
    }

    @Override
    public WorkflowInstanceDTO convertToDTO(WorkflowInstance workflow) {
        return new WorkflowInstanceDTO(
                workflow.getId(),
                workflow.getDocumentId(),
                workflow.getWorkflowType(),
                workflow.getCurrentStatus(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt()
        );
    }
    
    @Override
    @Transactional
    public WorkflowInstance processNextStep(UUID documentId, String actionData) {
        WorkflowInstance workflow = workflowRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Workflow for document ID " + documentId + " not found"));
        
        log.info("Processing next step for document ID: {} with current status: {}", 
                documentId, workflow.getCurrentStatus());
        
        // Determine and execute the next step based on current status
        switch (workflow.getCurrentStatus()) {
            case SUBMITTED:
                // If already submitted and it's a creation workflow, publish it
                if (workflow.getWorkflowType() == WorkflowType.DOCUMENT_CREATION) {
                    workflow.setCurrentStatus(WorkflowStatus.PUBLISHED);
                    // Notify Documents service that the document is validated and ready
                    kafkaProducerService.publishDocumentValidated(documentId, "{}");
                    // Also publish the document
                    kafkaProducerService.publishDocumentPublished(documentId, "{}");
                    log.info("Document ID: {} automatically published as it was created directly", documentId);
                }
                break;
                
            case FIELD_EXTRACTION_PENDING:
                workflow.setCurrentStatus(WorkflowStatus.VALIDATION_PENDING);
                
                // Send the extracted fields to the Documents service if provided
                if (actionData != null && !actionData.isEmpty()) {
                    kafkaProducerService.publishExtractedFields(documentId, actionData);
                } else {
                    // Use a placeholder if no data provided
                    kafkaProducerService.publishExtractedFields(documentId, 
                        "{\"extractedFields\": {\"documentType\": \"Auto-detected\"}}");
                }
                log.info("Document ID: {} field extraction completed, now pending validation", documentId);
                break;
                
            case VALIDATION_PENDING:
                // Doctor validates the extraction
                workflow.setCurrentStatus(WorkflowStatus.VALIDATED);
                
                // Notify Documents service about the validation with validated data if provided
                if (actionData != null && !actionData.isEmpty()) {
                    kafkaProducerService.publishDocumentValidated(documentId, actionData);
                } else {
                    kafkaProducerService.publishDocumentValidated(documentId, "{}");
                }
                log.info("Document ID: {} field extraction validated by doctor", documentId);
                break;
                
            case VALIDATED:
                // Move to published state after validation
                workflow.setCurrentStatus(WorkflowStatus.PUBLISHED);
                // Send the published event
                kafkaProducerService.publishDocumentPublished(documentId, "{}");
                log.info("Document ID: {} has been published after validation", documentId);
                break;
                
            case PUBLISHED:
                // Already at final state
                log.info("Document ID: {} is already published - no further action needed", documentId);
                break;
                
            case REJECTED:
                // Can't proceed from rejected state
                log.info("Document ID: {} is rejected - no automatic next step available", documentId);
                break;
                
            default:
                log.warn("Unknown status for document ID: {}", documentId);
                break;
        }
        
        return workflowRepository.save(workflow);
    }
}