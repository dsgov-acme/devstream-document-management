package io.nuvalence.ds4g.documentmanagement.service.processor;

import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorResult;
import jakarta.validation.constraints.NotNull;

/**
 *  Interface for document processors.
 */
public interface DocumentProcessor {
    String getProcessorId();

    @NotNull DocumentProcessorResult process(String documentId);
}
