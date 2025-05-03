package com.example.workflow.repository;

import com.example.workflow.model.WorkflowInstance;
import com.example.workflow.model.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowInstance, Long> {
    
    Optional<WorkflowInstance> findByDocumentId(Long documentId);
    
    List<WorkflowInstance> findByCurrentStatus(WorkflowStatus status);
    
    List<WorkflowInstance> findByDocumentIdIn(List<Long> documentIds);
}