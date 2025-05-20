package com.example.documents.repository;

import com.example.documents.model.Document;
import com.example.documents.model.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    
    // Find documents by patient ID
    List<Document> findByPatientId(String patientId);
    
    // Find documents by status using enum
    List<Document> findByStatus(DocumentStatus status);
    
    // Find documents by patient ID and status
    List<Document> findByPatientIdAndStatus(String patientId, DocumentStatus status);
    
    // Find documents by diagnosis
    List<Document> findByDiagnosis(String diagnosis);
}