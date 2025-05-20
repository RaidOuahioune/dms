package com.example.documents.listener;

import com.example.documents.dto.DocumentDTO;
import com.example.documents.model.Document;
import com.example.documents.model.DocumentStatus;
import com.example.documents.repository.DocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kafka listener for document extraction responses
 * Processes extraction results and updates documents with extracted data
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExtractionListener {

    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC_DOCUMENT_FIELDS_EXTRACTED = "document-fields-extracted";

    /**
     * Listen for extraction responses from the extractor service
     */
    @KafkaListener(topics = "extraction_response", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleExtractionResponse(String message) {
        log.info("Received extraction response: {}", message);
        
        try {
            // Parse the message
            JsonNode responseJson = objectMapper.readTree(message);
            String documentIdStr = responseJson.get("document_id").asText();
            UUID documentId = UUID.fromString(documentIdStr);
            
            // Get the formatted extraction result
            String formattedContent = responseJson.get("formatted").asText();
            
            log.info("Processing extraction for document ID: {}", documentId);
            
            // Retrieve the document from the database
            documentRepository.findById(documentId).ifPresentOrElse(
                document -> {
                    try {
                        // Parse the formatted content
                        JsonNode extractedData = objectMapper.readTree(formattedContent);
                        
                        // Apply extracted data to document
                        updateDocumentWithExtractedData(document, extractedData);
                        
                        // Save the document with extracted data
                        Document savedDocument = documentRepository.save(document);
                        
                        // Notify workflow service that fields were extracted
                        notifyFieldsExtracted(savedDocument);
                        
                        log.info("Document {} updated with extracted data", documentId);
                    } catch (JsonProcessingException e) {
                        log.error("Error parsing extraction response for document {}: {}", documentId, e.getMessage(), e);
                    }
                },
                () -> log.error("Document with ID {} not found", documentId)
            );
        } catch (Exception e) {
            log.error("Error processing extraction response: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update document with extracted data from medical document
     */
    private void updateDocumentWithExtractedData(Document document, JsonNode extractedData) {
        try {
            // Since Document model doesn't have extractedMetadata field, we'll store relevant data in the description
            StringBuilder enhancedDescription = new StringBuilder(document.getDescription() != null ? document.getDescription() : "");
            enhancedDescription.append("\n\n--- EXTRACTED DATA ---\n");
            enhancedDescription.append(extractedData.toString());
            document.setDescription(enhancedDescription.toString());
            
            // Process procedure date
            if (extractedData.has("date") && !extractedData.get("date").isNull()) {
                String dateStr = extractedData.get("date").asText();
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    document.setProcedureDate(date.atStartOfDay());
                } catch (DateTimeParseException e) {
                    log.warn("Invalid date format in extraction: {}", dateStr);
                }
            }
            
            // Process patient ID
            if (extractedData.has("patientId") && !extractedData.get("patientId").isNull()) {
                // Update patient ID only if it wasn't already set
                if (document.getPatientId() == null || document.getPatientId().trim().isEmpty()) {
                    String patientId = extractedData.get("patientId").asText();
                    document.setPatientId(patientId);
                }
            }
            
            // Process doctor IDs
            if (extractedData.has("operators") && extractedData.get("operators").isArray()) {
                JsonNode operators = extractedData.get("operators");
                List<String> operatorsList = new ArrayList<>();
                operators.forEach(operator -> operatorsList.add(operator.asText()));
                // Store operators as comma-separated list in doctorIds field
                document.setDoctorIds(String.join(",", operatorsList));
            }
            
            // Process diagnosis if available
            if (extractedData.has("diagnosis") && !extractedData.get("diagnosis").isNull()) {
                String diagnosis = extractedData.get("diagnosis").asText();
                document.setDiagnosis(diagnosis);
            }
            
            // Update document status
            document.setStatus(DocumentStatus.PENDING); // Keep as PENDING during processing
            document.setStatusUpdatedAt(LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Error updating document with extracted data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Notify the workflow service that fields were extracted
     */
    private void notifyFieldsExtracted(Document document) {
        DocumentDTO documentDTO = mapToDTO(document);
        kafkaTemplate.send(TOPIC_DOCUMENT_FIELDS_EXTRACTED, documentDTO);
        log.info("Sent document fields extracted event for document ID: {}", document.getId());
    }
    
    /**
     * Map Document entity to DocumentDTO
     */
    private DocumentDTO mapToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .patientId(document.getPatientId())
                .diagnosis(document.getDiagnosis())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .statusUpdatedAt(document.getStatusUpdatedAt())
                .procedureDate(document.getProcedureDate())
                .doctorIds(document.getDoctorIds())
                .description(document.getDescription())
                .status(document.getStatus())
                .build();
    }
}