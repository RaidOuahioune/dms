package com.example.workflow.dto;

import com.example.workflow.model.WorkflowStatus;
import com.example.workflow.model.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceDTO {
    private Long id;
    private Long documentId;
    private WorkflowType workflowType;
    private WorkflowStatus currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}