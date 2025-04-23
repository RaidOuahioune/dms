package com.example.documents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for document creation and updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String content;
    
    @NotBlank(message = "Document type is required")
    private String type;
    
    @NotBlank(message = "Patient ID is required")
    private String patientId;
    
    @NotBlank(message = "Doctor ID is required")
    private String doctorId;
    
    private String department;
    private String specialty;
    private String status;
}