package com.example.documents.controller;

import com.example.documents.dto.DocumentDTO;
import com.example.documents.dto.response.ApiResponse;
import com.example.documents.security.JwtTokenProvider;
import com.example.documents.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Controller for handling medical document uploads and processing
 */
@RestController
@RequestMapping("/api/v1/medical-documents")
@RequiredArgsConstructor
@Slf4j
public class MedicalDocumentController {

    private final DocumentService documentService;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * Upload and process a medical Word document
     * This endpoint accepts a Word document, extracts its content, 
     * and sends it to the extractor service for field extraction.
     * 
     * @param file The Word document file
     * @param patientId The ID of the patient this document is for
     * @param documentType The type of medical document (e.g., CARDIOLOGY_REPORT)
     * @param request HttpServletRequest for extracting the authenticated user
     * @return ResponseEntity with the created document
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDTO>> uploadMedicalDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("patientId") String patientId,
            @RequestParam("documentType") String documentType,
            HttpServletRequest request) {
        
        try {
            // Validate file type
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.toLowerCase().endsWith(".doc") && !filename.toLowerCase().endsWith(".docx"))) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Only .doc and .docx files are supported"));
            }
            
            // Extract authenticated user ID as the doctor ID
            String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
            String doctorId = jwtTokenProvider.extractUserIdFromToken(token);
            
            // Process the document
            DocumentDTO document = documentService.uploadMedicalDocument(file, patientId, doctorId, documentType);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Medical document uploaded successfully and sent for extraction", document));
            
        } catch (IOException e) {
            log.error("Error processing document upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Error processing document: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during document upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "An unexpected error occurred"));
        }
    }
}