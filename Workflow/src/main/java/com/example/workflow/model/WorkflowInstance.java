package com.example.workflow.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_instances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long documentId;
    
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