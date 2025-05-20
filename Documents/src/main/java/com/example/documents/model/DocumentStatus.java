package com.example.documents.model;

/**
 * Enum representing the possible statuses of a document in the system.
 * Each status represents a specific stage in the document lifecycle.
 */
public enum DocumentStatus {
    /**
     * Document is waiting for validation by an authorized user
     */
    PENDING,
    
    /**
     * Document has been validated and approved
     */
    VALIDATED,
    
    /**
     * Document has been rejected and requires revision
     */
    REJECTED
}
