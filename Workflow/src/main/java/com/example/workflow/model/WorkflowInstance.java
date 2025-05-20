package com.example.workflow.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_instances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstance {
    
    @Id
    @GeneratedValue
    @Column(columnDefinition = "TEXT")
    private UUID id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private UUID documentId;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkflowType workflowType;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkflowStatus currentStatus;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}