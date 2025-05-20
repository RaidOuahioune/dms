package com.example.documents.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document entity representing medical documents in the system
 */
@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(nullable = false)
    private String title;
    
    @Column
    private String patientId; // Reference to Patient in patient service, nullable as requested
    
    @Column(columnDefinition = "TEXT")
    private String diagnosis;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "procedure_date")
    private LocalDateTime procedureDate;
    
    // Store doctor IDs as a comma-separated string that can be parsed to a list
    @Column(name = "doctor_ids", columnDefinition = "TEXT")
    private String doctorIds; // Nullable as requested
    
  
    @Column(columnDefinition = "TEXT")
    private String description; // HTML content
    
    @Enumerated(EnumType.STRING)
    @Column
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING; // Default to PENDING, nullable as requested
    
    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt; // Tracks when the status was last updated
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        statusUpdatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}