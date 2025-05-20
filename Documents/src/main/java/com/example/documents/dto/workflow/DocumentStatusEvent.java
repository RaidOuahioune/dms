package com.example.documents.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Data Transfer Object for receiving document status events from the Workflow service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatusEvent {
    private UUID documentId;
    private String status;
    private String data;
}
