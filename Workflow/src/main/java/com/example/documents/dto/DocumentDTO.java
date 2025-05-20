package com.example.documents.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Document entity
 * This is a copy of the original DocumentDTO from the Documents service
 * to support Kafka deserialization in the Workflow service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private UUID id;
    private String title;
    private String patientId;
    private String diagnosis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime statusUpdatedAt;
    private LocalDateTime procedureDate;
    private String doctorIds; // Comma-separated list of doctor IDs
    private String description; // HTML content that may also contain the document's content
    private DocumentStatus status;
}
