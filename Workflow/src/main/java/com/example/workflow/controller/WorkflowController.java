package com.example.workflow.controller;

import com.example.workflow.dto.WorkflowInfoResponse;
import com.example.workflow.dto.WorkflowInstanceDTO;
import com.example.workflow.model.WorkflowStatus;
import com.example.workflow.service.KafkaProducerService;
import com.example.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {
    
    private final WorkflowService workflowService;
    private final KafkaProducerService kafkaProducerService;
    
    @GetMapping("/document/{documentId}")
    public ResponseEntity<WorkflowInstanceDTO> getWorkflowByDocumentId(@PathVariable UUID documentId) {
        return workflowService.getWorkflowByDocumentId(documentId)
                .map(workflow -> ResponseEntity.ok(workflowService.convertToDTO(workflow)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<WorkflowInstanceDTO>> getWorkflowsByStatus(@PathVariable WorkflowStatus status) {
        List<WorkflowInstanceDTO> workflows = workflowService.getWorkflowsByStatus(status)
                .stream()
                .map(workflowService::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(workflows);
    }
    
    @PutMapping("/{workflowId}/validate")
    public ResponseEntity<WorkflowInstanceDTO> validateDocumentFields(@PathVariable UUID workflowId) {
        var updatedWorkflow = workflowService.updateWorkflowStatus(workflowId, WorkflowStatus.VALIDATED);
        return ResponseEntity.ok(workflowService.convertToDTO(updatedWorkflow));
    }
    
    @PutMapping("/{workflowId}/publish")
    public ResponseEntity<WorkflowInstanceDTO> publishDocument(@PathVariable UUID workflowId) {
        var updatedWorkflow = workflowService.updateWorkflowStatus(workflowId, WorkflowStatus.PUBLISHED);
        return ResponseEntity.ok(workflowService.convertToDTO(updatedWorkflow));
    }
    
    @PutMapping("/{workflowId}/reject")
    public ResponseEntity<WorkflowInstanceDTO> rejectDocument(@PathVariable UUID workflowId) {
        var updatedWorkflow = workflowService.updateWorkflowStatus(workflowId, WorkflowStatus.REJECTED);
        return ResponseEntity.ok(workflowService.convertToDTO(updatedWorkflow));
    }
    
    /**
     * Endpoint to send extracted data directly to Documents service but not store it
     */
    @PostMapping("/document/{documentId}/extracted-data")
    public ResponseEntity<WorkflowInstanceDTO> sendExtractedData(
            @PathVariable UUID documentId, 
            @RequestBody String extractedData) {
        
        // Send extracted data to Documents service
        kafkaProducerService.publishExtractedFields(documentId, extractedData);
        
        // Just update workflow status 
        var updatedWorkflow = workflowService.processNextStep(documentId, null);
        return ResponseEntity.ok(workflowService.convertToDTO(updatedWorkflow));
    }
    
    /**
     * Stateful endpoint that automatically processes the next step in the document workflow
     */
    @PostMapping("/document/{documentId}/next")
    public ResponseEntity<WorkflowInstanceDTO> processNextStep(
            @PathVariable UUID documentId,
            @RequestBody(required = false) String actionData) {
        var updatedWorkflow = workflowService.processNextStep(documentId, actionData);
        return ResponseEntity.ok(workflowService.convertToDTO(updatedWorkflow));
    }
    
    /**
     * Get information about the current step and what's next in the workflow
     */
    @GetMapping("/document/{documentId}/workflow-info")
    public ResponseEntity<WorkflowInfoResponse> getWorkflowInfo(@PathVariable UUID documentId) {
        return workflowService.getWorkflowByDocumentId(documentId)
                .map(workflow -> {
                    var status = workflow.getCurrentStatus();
                    var nextAction = getNextActionDescription(status);
                    
                    WorkflowInfoResponse response = new WorkflowInfoResponse(
                        workflow.getDocumentId(),
                        status,
                        nextAction,
                        status == WorkflowStatus.PUBLISHED
                    );
                    
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Helper method to determine the next action description based on current status
     */
    private String getNextActionDescription(WorkflowStatus status) {
        return switch(status) {
            case SUBMITTED -> "Publish the document";
            case FIELD_EXTRACTION_PENDING -> "Extract document fields with AI";
            case VALIDATION_PENDING -> "Validate extracted fields";
            case VALIDATED -> "Publish the document";
            case PUBLISHED -> "No further actions needed";
            case REJECTED -> "Document rejected - no further actions available";
            default -> "Unknown status";
        };
    }
}