package com.example.workflow.model;

public enum WorkflowStatus {
    SUBMITTED,
    FIELD_EXTRACTION_PENDING,
    VALIDATION_PENDING,
    VALIDATED,
    PUBLISHED,
    REJECTED
}