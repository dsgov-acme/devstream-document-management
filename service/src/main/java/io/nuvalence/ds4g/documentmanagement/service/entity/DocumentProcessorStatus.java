package io.nuvalence.ds4g.documentmanagement.service.entity;

/**
 * Enum for DocumentProcessor Result statuses.
 */
public enum DocumentProcessorStatus {
    COMPLETE,
    UNPROCESSABLE,

    RETRYABLE_ERROR,
    PENDING, // intended for enqueueing (should we change this to QUEUED? or SCHEDULED?)
    MISSING_DEPENDENCY
}
