package com.example.documents.service;

import com.example.documents.dto.DocumentDTO;
import com.example.documents.dto.DocumentRequest;
import com.example.documents.model.Document;
import com.example.documents.repository.DocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final KafkaTemplate<String, DocumentDTO> kafkaTemplate;
    
    private static final String TOPIC_DOCUMENT_CREATED = "document-created";
    private static final String TOPIC_DOCUMENT_UPDATED = "document-updated";
    private static final String TOPIC_DOCUMENT_DELETED = "document-deleted";

    @Override
    public List<DocumentDTO> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentDTO getDocumentById(UUID id) {
        return documentRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with ID: " + id));
    }

    @Override
    @Transactional
    public DocumentDTO createDocument(DocumentRequest documentRequest, String createdByUserId) {
        Document document = Document.builder()
                .title(documentRequest.getTitle())
                .content(documentRequest.getContent())
                .type(documentRequest.getType())
                .patientId(documentRequest.getPatientId())
                .doctorId(documentRequest.getDoctorId() != null ? documentRequest.getDoctorId() : createdByUserId)
                .department(documentRequest.getDepartment())
                .specialty(documentRequest.getSpecialty())
                .status(documentRequest.getStatus() != null ? documentRequest.getStatus() : "DRAFT")
                .build();
                
        Document savedDocument = documentRepository.save(document);
        DocumentDTO documentDTO = mapToDTO(savedDocument);
        
        // Publish the event to Kafka
        kafkaTemplate.send(TOPIC_DOCUMENT_CREATED, documentDTO);
        
        return documentDTO;
    }

    @Override
    @Transactional
    public DocumentDTO updateDocument(UUID id, DocumentRequest documentRequest, String updatedByUserId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with ID: " + id));
                
        document.setTitle(documentRequest.getTitle());
        document.setContent(documentRequest.getContent());
        document.setType(documentRequest.getType());
        document.setPatientId(documentRequest.getPatientId());
        document.setDoctorId(documentRequest.getDoctorId());
        document.setDepartment(documentRequest.getDepartment());
        document.setSpecialty(documentRequest.getSpecialty());
        document.setStatus(documentRequest.getStatus());
        
        Document updatedDocument = documentRepository.save(document);
        DocumentDTO documentDTO = mapToDTO(updatedDocument);
        
        // Publish the event to Kafka
        kafkaTemplate.send(TOPIC_DOCUMENT_UPDATED, documentDTO);
        
        return documentDTO;
    }

    @Override
    @Transactional
    public void deleteDocument(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with ID: " + id));
        
        documentRepository.delete(document);
        
        // Publish the event to Kafka
        kafkaTemplate.send(TOPIC_DOCUMENT_DELETED, mapToDTO(document));
    }

    @Override
    public List<DocumentDTO> getDocumentsByPatientId(String patientId) {
        return documentRepository.findByPatientId(patientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentDTO> getDocumentsByDoctorId(String doctorId) {
        return documentRepository.findByDoctorId(doctorId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentDTO> getDocumentsByType(String type) {
        return documentRepository.findByType(type).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentDTO> getDocumentsByDepartment(String department) {
        return documentRepository.findByDepartment(department).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    private DocumentDTO mapToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .content(document.getContent())
                .type(document.getType())
                .patientId(document.getPatientId())
                .doctorId(document.getDoctorId())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .department(document.getDepartment())
                .specialty(document.getSpecialty())
                .status(document.getStatus())
                .build();
    }
}