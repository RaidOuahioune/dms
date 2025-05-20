package com.example.workflow.dto;

import com.example.workflow.model.WorkflowStatus;
import com.example.workflow.model.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.id.uuid.UuidValueGenerator;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceDTO {
    private UUID id;
    private UUID documentId;
    private WorkflowType workflowType;
    private WorkflowStatus currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}