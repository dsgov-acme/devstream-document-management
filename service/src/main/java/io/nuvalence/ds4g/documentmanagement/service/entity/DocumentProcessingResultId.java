package io.nuvalence.ds4g.documentmanagement.service.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite id for document processing results.
 */
@Data
public class DocumentProcessingResultId implements Serializable {
    private static final long serialVersionUID = -4037562299571005537L;
    private String processorId;
    private UUID documentId;
}
