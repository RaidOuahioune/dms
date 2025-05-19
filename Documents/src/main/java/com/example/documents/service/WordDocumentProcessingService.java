package com.example.documents.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service for processing Word documents (.doc and .docx files)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordDocumentProcessingService {

    /**
     * Extract plain text content from a Word document
     * @param file The uploaded Word document file
     * @return The extracted text content
     * @throws IOException If there's an error reading the file
     */
    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        log.info("Extracting text from Word document: {}", fileName);
        
        try (InputStream inputStream = file.getInputStream()) {
            String text;
            
            if (fileName != null && fileName.toLowerCase().endsWith(".docx")) {
                // Process DOCX (Word 2007+) files
                try (XWPFDocument document = new XWPFDocument(inputStream)) {
                    XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                    text = extractor.getText();
                    extractor.close();
                }
            } else if (fileName != null && fileName.toLowerCase().endsWith(".doc")) {
                // Process DOC (Word 97-2003) files
                try (HWPFDocument document = new HWPFDocument(inputStream)) {
                    WordExtractor extractor = new WordExtractor(document);
                    text = extractor.getText();
                    extractor.close();
                }
            } else {
                throw new IllegalArgumentException("Unsupported file format. Only .doc and .docx files are supported.");
            }
            
            log.info("Successfully extracted {} characters from {}", text.length(), fileName);
            return text;
        } catch (Exception e) {
            log.error("Error extracting text from Word document: {}", e.getMessage(), e);
            throw new IOException("Error processing Word document: " + e.getMessage(), e);
        }
    }
}