package com.example.workflow.dto;

import java.util.UUID;

import com.example.workflow.model.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for workflow information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInfoResponse {
    private UUID documentId;
    private WorkflowStatus currentStatus;
    private String nextActionDescription;
    private boolean isComplete;
}