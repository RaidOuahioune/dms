package com.example.documents.controller;

import com.example.documents.dto.DocumentDTO;
import com.example.documents.dto.response.ApiResponse;
import com.example.documents.model.DocumentStatus;
import com.example.documents.security.JwtTokenProvider;
import com.example.documents.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for medical records and reports with specialized fields
 */
@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
@Slf4j
public class MedicalRecordController {

    private final DocumentService documentService;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * Get all medical records (only documents of type MEDICAL_RECORD or similar)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getAllMedicalRecords() {
        List<DocumentDTO> medicalRecords = documentService.getDocumentsByType("MEDICAL_RECORD");
        return ResponseEntity.ok(ApiResponse.success("Medical records retrieved successfully", medicalRecords));
    }
    
    /**
     * Get a specific medical record by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> getMedicalRecordById(@PathVariable UUID id) {
        DocumentDTO document = documentService.getDocumentById(id);
        return ResponseEntity.ok(ApiResponse.success("Medical record retrieved successfully", document));
    }
    
    /**
     * Get all medical records for a specific patient
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getMedicalRecordsByPatientId(
            @PathVariable String patientId) {
        List<DocumentDTO> documents = documentService.getDocumentsByPatientId(patientId);
        // Filter to include only medical records
        List<DocumentDTO> medicalRecords = documents.stream()
                .filter(doc -> doc.getDiagnosis() != null && 
                              ("MEDICAL_RECORD".equals(doc.getDiagnosis()) || 
                               "CARDIOLOGY_REPORT".equals(doc.getDiagnosis())))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Patient medical records retrieved successfully", medicalRecords));
    }
    
    /**
     * Get a list of operators mentioned in a medical record
     */
    @GetMapping("/{id}/operators")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getMedicalRecordOperators(@PathVariable UUID id) {
        DocumentDTO document = documentService.getDocumentById(id);
        
        // Extract doctor IDs from the document
        List<String> operators = documentService.extractDoctorIdsFromDocument(document);
        
        return ResponseEntity.ok(ApiResponse.success("Medical record operators retrieved successfully", operators));
    }
    
    /**
     * Manually validate a medical record after reviewing the extracted information
     */
    @PutMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> validateMedicalRecord(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Object> updates) {
        
        // Update the document status to VALIDATED
        DocumentDTO updatedDocument = documentService.updateDocumentStatus(id, DocumentStatus.VALIDATED, null);
        return ResponseEntity.ok(ApiResponse.success("Medical record validated successfully", updatedDocument));
    }
    
    /**
     * Reject a medical record if the extracted information is incorrect
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> rejectMedicalRecord(
            @PathVariable UUID id,
            @RequestBody(required = false) String reason) {
        
        // Update the document status to REJECTED
        DocumentDTO updatedDocument = documentService.updateDocumentStatus(id, DocumentStatus.REJECTED, reason);
        return ResponseEntity.ok(ApiResponse.success("Medical record rejected", updatedDocument));
    }
}