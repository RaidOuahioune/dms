package com.example.documents.service;

import com.example.documents.dto.DocumentDTO;
import com.example.documents.dto.DocumentRequest;
import com.example.documents.model.DocumentStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface DocumentService {
    
    List<DocumentDTO> getAllDocuments();
    
    DocumentDTO getDocumentById(UUID id);
    
    DocumentDTO createDocument(DocumentRequest documentRequest, String createdByUserId);
    
    DocumentDTO updateDocument(UUID id, DocumentRequest documentRequest, String updatedByUserId);
    
    void deleteDocument(UUID id);
    
    List<DocumentDTO> getDocumentsByPatientId(String patientId);
    
    /**
     * Get documents that include a specific doctor in the doctorIds list
     * @param doctorId The doctor ID to search for
     * @return List of documents associated with the doctor
     */
    List<DocumentDTO> getDocumentsByDoctorId(String doctorId);
    
    /**
     * Get documents by status
     * @param status The status to filter by
     * @return List of documents with the specified status
     */
    List<DocumentDTO> getDocumentsByStatus(DocumentStatus status);
    
    /**
     * Get documents by type
     * @param type The document type to filter by
     * @return List of documents with the specified type
     */
    List<DocumentDTO> getDocumentsByType(String type);
    
    /**
     * Get documents by department
     * @param department The department to filter by
     * @return List of documents from the specified department
     */
    List<DocumentDTO> getDocumentsByDepartment(String department);
    
    /**
     * Update document status
     * @param id Document ID
     * @param status New status
     * @param reason Optional reason for status change (especially for rejection)
     * @return Updated document DTO
     */
    DocumentDTO updateDocumentStatus(UUID id, DocumentStatus status, String reason);
    
    /**
     * Upload and process a medical document
     * @param file The document file
     * @param patientId The ID of the patient this document is for
     * @param doctorId The ID of the primary doctor
     * @param diagnosis The initial diagnosis
     * @return The created document DTO
     * @throws IOException If there's an error processing the file
     */
    DocumentDTO uploadMedicalDocument(MultipartFile file, String patientId, String doctorId, String diagnosis) throws IOException;
    
    /**
     * Parse the doctorIds string into a list of doctor IDs
     * @param document The document containing doctorIds
     * @return List of doctor IDs
     */
    List<String> extractDoctorIdsFromDocument(DocumentDTO document);
}