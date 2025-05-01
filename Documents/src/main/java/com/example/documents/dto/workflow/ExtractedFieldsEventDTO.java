package com.example.documents.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * DTO for field extraction events from the Workflow service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExtractedFieldsEventDTO extends WorkflowEventDTO {
    private String extractedFields; // JSON string containing extracted fields
}