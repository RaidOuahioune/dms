package com.example.documents.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Data Transfer Object for workflow events
 * Using UUID for documentId to match the updated Workflow service data type
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEventDTO {
    private UUID documentId;
    private String eventType;
    private String data;
}