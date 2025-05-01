package com.example.documents.model;

/**
 * Enum representing the possible statuses of a document
 */
public enum DocumentStatus {
    PENDING,     // Document is waiting for workflow processing
    PROCESSING,  // Document is being processed by workflow
    VALIDATED,   // Document has been validated by workflow
    PUBLISHED,   // Document has completed workflow and is published
    REJECTED,    // Document has been rejected in the workflow
    DRAFT,       // Document is in draft state
    ARCHIVED     // Document has been archived
}
