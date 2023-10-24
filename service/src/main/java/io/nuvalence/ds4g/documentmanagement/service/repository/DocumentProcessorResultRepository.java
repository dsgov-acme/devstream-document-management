package io.nuvalence.ds4g.documentmanagement.service.repository;

import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessingResultId;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 *  Repository for Document processing results.
 */
@Repository
public interface DocumentProcessorResultRepository
        extends JpaRepository<DocumentProcessorResult, DocumentProcessingResultId> {
    List<DocumentProcessorResult> findByDocumentId(UUID id);

    List<DocumentProcessorResult> findByDocumentIdAndProcessorIdIn(
            UUID documentId, List<String> processorIds);
}
