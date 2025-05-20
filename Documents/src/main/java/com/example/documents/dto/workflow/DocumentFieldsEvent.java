package com.example.documents.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for receiving document field extraction events from the Workflow service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFieldsEvent {
    private Long documentId;
    private String extractedFields;
}
