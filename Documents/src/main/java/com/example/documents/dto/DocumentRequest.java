package com.example.documents.dto;

import com.example.documents.model.DocumentStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    
    private String patientId;
    
    private String diagnosis;
    
    private LocalDateTime procedureDate;
    
    private String doctorIds; // Comma-separated list of doctor IDs
    
    private String content;
    
    private String description; // HTML content
    
    private DocumentStatus status;
}