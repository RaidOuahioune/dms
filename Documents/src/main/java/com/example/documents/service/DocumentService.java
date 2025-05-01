package com.example.documents.service;

import com.example.documents.dto.DocumentDTO;
import com.example.documents.dto.DocumentRequest;
import com.example.documents.model.DocumentStatus;

import java.util.List;
import java.util.UUID;

public interface DocumentService {
    
    List<DocumentDTO> getAllDocuments();
    
    DocumentDTO getDocumentById(UUID id);
    
    DocumentDTO createDocument(DocumentRequest documentRequest, String createdByUserId);
    
    DocumentDTO updateDocument(UUID id, DocumentRequest documentRequest, String updatedByUserId);
    
    void deleteDocument(UUID id);
    
    List<DocumentDTO> getDocumentsByPatientId(String patientId);
    
    List<DocumentDTO> getDocumentsByDoctorId(String doctorId);
    
    List<DocumentDTO> getDocumentsByType(String type);
    
    List<DocumentDTO> getDocumentsByDepartment(String department);
    
    /**
     * Get documents by status
     * @param status The status to filter by
     * @return List of documents with the specified status
     */
    List<DocumentDTO> getDocumentsByStatus(DocumentStatus status);
    
    /**
     * Update document status and optionally metadata
     * @param id Document ID
     * @param status New status
     * @param metadata Optional JSON metadata to update
     * @return Updated document DTO
     */
    DocumentDTO updateDocumentStatus(UUID id, DocumentStatus status, String metadata);
}