package com.example.documents.service;

import com.example.documents.dto.DocumentDTO;
import com.example.documents.dto.DocumentRequest;

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
}