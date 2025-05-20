package com.example.documents.config;

import com.example.documents.model.Document;
import com.example.documents.model.DocumentStatus;
import com.example.documents.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DocumentSeeder implements CommandLineRunner {
    private final DocumentRepository documentRepository;

    @Override
    public void run(String... args) {
        if (documentRepository.count() == 0) {
            Document doc1 = Document.builder()
                    .title("Cardiology Report")
                    .patientId("PATIENT-001")
                    .doctorIds("DR-101,DR-102")
                    .diagnosis("CARDIOLOGY_REPORT")
                    .description("Patient has a mild arrhythmia. Further monitoring advised.")
                    .procedureDate(LocalDateTime.now().minusDays(10))
                    .status(DocumentStatus.PENDING)
                    .statusUpdatedAt(LocalDateTime.now().minusDays(10))
                    .build();

            Document doc2 = Document.builder()
                    .title("Medical Record - Diabetes")
                    .patientId("PATIENT-002")
                    .doctorIds("DR-103")
                    .diagnosis("MEDICAL_RECORD")
                    .description("Patient diagnosed with Type 2 Diabetes. Medication prescribed.")
                    .procedureDate(LocalDateTime.now().minusDays(20))
                    .status(DocumentStatus.VALIDATED)
                    .statusUpdatedAt(LocalDateTime.now().minusDays(5))
                    .build();

            Document doc3 = Document.builder()
                    .title("Annual Physical Exam")
                    .patientId("PATIENT-003")
                    .doctorIds("DR-104")
                    .diagnosis("PHYSICAL_EXAM")
                    .description("All vitals normal. No concerns.")
                    .procedureDate(LocalDateTime.now().minusDays(30))
                    .status(DocumentStatus.REJECTED)
                    .statusUpdatedAt(LocalDateTime.now().minusDays(2))
                    .build();

            documentRepository.saveAll(Arrays.asList(doc1, doc2, doc3));
        }
    }
}
