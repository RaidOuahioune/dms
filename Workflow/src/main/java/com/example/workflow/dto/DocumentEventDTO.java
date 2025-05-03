package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEventDTO {
    private Long id;
    private String title;
    private String fileName;
    private String contentType;
    private String fileData; // Base64 encoded file data if present
}