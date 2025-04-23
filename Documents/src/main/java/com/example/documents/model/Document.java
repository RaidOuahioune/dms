package com.example.documents.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

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
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false)
    private String type; // e.g., "MEDICAL_RECORD", "LAB_RESULT", "PRESCRIPTION"
    
    // External references - avoiding content coupling by using IDs instead of entities
    @Column(nullable = false)
    private String patientId; // Reference to Patient in patient service
    
    @Column(nullable = false)
    private String doctorId; // Reference to Doctor in doctor/auth service
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Metadata fields
    private String department;
    private String specialty;
    private String status; // e.g., "DRAFT", "FINAL", "ARCHIVED"
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}