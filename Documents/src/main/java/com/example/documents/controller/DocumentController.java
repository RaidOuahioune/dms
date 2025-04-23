package com.example.documents.controller;

import com.example.documents.dto.DocumentDTO;
import com.example.documents.dto.DocumentRequest;
import com.example.documents.dto.response.ApiResponse;
import com.example.documents.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getAllDocuments() {
        List<DocumentDTO> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", documents));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> getDocumentById(@PathVariable UUID id) {
        DocumentDTO document = documentService.getDocumentById(id);
        return ResponseEntity.ok(ApiResponse.success("Document retrieved successfully", document));
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> createDocument(
            @Valid @RequestBody DocumentRequest documentRequest,
            Authentication authentication) {
        String userId = authentication.getName();
        DocumentDTO createdDocument = documentService.createDocument(documentRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document created successfully", createdDocument));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> updateDocument(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentRequest documentRequest,
            Authentication authentication) {
        String userId = authentication.getName();
        DocumentDTO updatedDocument = documentService.updateDocument(id, documentRequest, userId);
        return ResponseEntity.ok(ApiResponse.success("Document updated successfully", updatedDocument));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }
    
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getDocumentsByPatientId(
            @PathVariable String patientId) {
        List<DocumentDTO> documents = documentService.getDocumentsByPatientId(patientId);
        return ResponseEntity.ok(ApiResponse.success("Patient documents retrieved successfully", documents));
    }
    
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getDocumentsByDoctorId(
            @PathVariable String doctorId) {
        List<DocumentDTO> documents = documentService.getDocumentsByDoctorId(doctorId);
        return ResponseEntity.ok(ApiResponse.success("Doctor's documents retrieved successfully", documents));
    }
    
    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getDocumentsByType(
            @PathVariable String type) {
        List<DocumentDTO> documents = documentService.getDocumentsByType(type);
        return ResponseEntity.ok(ApiResponse.success("Documents by type retrieved successfully", documents));
    }
    
    @GetMapping("/department/{department}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getDocumentsByDepartment(
            @PathVariable String department) {
        List<DocumentDTO> documents = documentService.getDocumentsByDepartment(department);
        return ResponseEntity.ok(ApiResponse.success("Department documents retrieved successfully", documents));
    }
}