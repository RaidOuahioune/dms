package com.example.workflow.service;

import com.example.workflow.dto.WorkflowInstanceDTO;
import com.example.workflow.model.WorkflowInstance;
import com.example.workflow.model.WorkflowStatus;
import com.example.workflow.model.WorkflowType;

import java.util.List;
import java.util.Optional;

public interface WorkflowService {
    
    WorkflowInstance createWorkflow(Long documentId, WorkflowType workflowType);
    
    WorkflowInstance updateWorkflowStatus(Long workflowId, WorkflowStatus newStatus);
    
    Optional<WorkflowInstance> getWorkflowByDocumentId(Long documentId);
    
    List<WorkflowInstance> getWorkflowsByStatus(WorkflowStatus status);
    
    WorkflowInstanceDTO convertToDTO(WorkflowInstance workflow);
    
    /**
     * Automatically processes the next step in the workflow based on current status
     * @param documentId The ID of the document
     * @param actionData Optional data needed for the next step (e.g., validation data)
     * @return Updated workflow instance after processing the next step
     */
    WorkflowInstance processNextStep(Long documentId, String actionData);
}