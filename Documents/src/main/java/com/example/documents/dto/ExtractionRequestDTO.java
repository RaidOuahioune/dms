package com.example.documents.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object for document extraction requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionRequestDTO {
    private UUID documentId;
    private String content;
}