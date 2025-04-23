package com.example.documents.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Document entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private UUID id;
    private String title;
    private String content;
    private String type;
    private String patientId;
    private String doctorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String department;
    private String specialty;
    private String status;
}