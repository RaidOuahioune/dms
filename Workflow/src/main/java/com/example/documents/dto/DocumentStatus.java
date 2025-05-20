package com.example.documents.dto;

/**
 * Enum representing the possible states of a document
 * This is a copy of the original DocumentStatus from the Documents service
 * to support Kafka deserialization in the Workflow service.
 */
public enum DocumentStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    ARCHIVED,
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    UNKNOWN
}
