package com.example.workflow.repository;

import com.example.workflow.model.WorkflowInstance;
import com.example.workflow.model.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowInstance, UUID> {
    
    Optional<WorkflowInstance> findByDocumentId(UUID documentId);
    
    List<WorkflowInstance> findByCurrentStatus(WorkflowStatus status);
    
    List<WorkflowInstance> findByDocumentIdIn(List<UUID> documentIds);
}