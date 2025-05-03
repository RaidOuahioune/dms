package com.example.documents.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base DTO for workflow events received from Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEventDTO {
    private UUID documentId;
    private String status;
    private String data;
    private LocalDateTime timestamp;
}