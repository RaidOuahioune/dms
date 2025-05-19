package com.example.documents.controller;

import com.example.documents.dto.DocumentDTO;
import com.example.documents.dto.DocumentRequest;
import com.example.documents.dto.response.ApiResponse;
import com.example.documents.model.DocumentStatus;
import com.example.documents.service.DocumentService;
import com.example.documents.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    private final JwtTokenProvider jwtTokenProvider;
    
    @GetMapping
    //@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getAllDocuments() {
        List<DocumentDTO> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", documents));
    }
    
    @GetMapping("/{id}")
    //@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> getDocumentById(@PathVariable UUID id) {
        DocumentDTO document = documentService.getDocumentById(id);
        return ResponseEntity.ok(ApiResponse.success("Document retrieved successfully", document));
    }
    
    @PostMapping
    //@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> createDocument(
            @Valid @RequestBody DocumentRequest documentRequest,
            HttpServletRequest request) {
        String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
        String userId = jwtTokenProvider.extractUserIdFromToken(token);
        
        DocumentDTO createdDocument = documentService.createDocument(documentRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document created successfully and sent for processing", createdDocument));
    }
    
    @PutMapping("/{id}")
    //@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> updateDocument(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentRequest documentRequest,
            HttpServletRequest request) {
        String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
        String userId = jwtTokenProvider.extractUserIdFromToken(token);
        
        DocumentDTO updatedDocument = documentService.updateDocument(id, documentRequest, userId);
        return ResponseEntity.ok(ApiResponse.success("Document updated successfully", updatedDocument));
    }
    
    @DeleteMapping("/{id}")
    //@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }
    
    @GetMapping("/patient/{patientId}")
    //@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getDocumentsByPatientId(
            @PathVariable String patientId) {
        List<DocumentDTO> documents = documentService.getDocumentsByPatientId(patientId);
        return ResponseEntity.ok(ApiResponse.success("Patient documents retrieved successfully", documents));
    }
    
    @GetMapping("/User/{UserId}")
    //@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getDocumentsByUserId(
            @PathVariable String UserId) {
        List<DocumentDTO> documents = documentService.getDocumentsByDoctorId(UserId);
        return ResponseEntity.ok(ApiResponse.success("User's documents retrieved successfully", documents));
    }
    
    @GetMapping("/type/{type}")
    //@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getDocumentsByType(
            @PathVariable String type) {
        List<DocumentDTO> documents = documentService.getDocumentsByType(type);
        return ResponseEntity.ok(ApiResponse.success("Documents by type retrieved successfully", documents));
    }
    
    @GetMapping("/department/{department}")
    //@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getDocumentsByDepartment(
            @PathVariable String department) {
        List<DocumentDTO> documents = documentService.getDocumentsByDepartment(department);
        return ResponseEntity.ok(ApiResponse.success("Department documents retrieved successfully", documents));
    }
    
    /**
     * Get documents by status
     */
    @GetMapping("/status/{status}")
    //@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentDTO>>> getDocumentsByStatus(
            @PathVariable DocumentStatus status) {
        List<DocumentDTO> documents = documentService.getDocumentsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(
            String.format("Documents with status %s retrieved successfully", status), documents));
    }
    
    /**
     * Update document status manually (Administrative function)
     */
    @PutMapping("/{id}/status")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> updateDocumentStatus(
            @PathVariable UUID id,
            @RequestParam DocumentStatus status,
            @RequestBody(required = false) String metadata) {
        
        DocumentDTO updatedDocument = documentService.updateDocumentStatus(id, status, metadata);
        return ResponseEntity.ok(ApiResponse.success(
            String.format("Document status updated to %s successfully", status), updatedDocument));
    }
    
    @GetMapping("/current-user")
    //@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(HttpServletRequest request) {
        String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
        String userId = jwtTokenProvider.extractUserIdFromToken(token);
        
        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", userId);
        userMap.put("userName", userDetails.getUsername());
        userMap.put("authority", userDetails.getAuthorities());
        
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved successfully", userMap));
    }
}