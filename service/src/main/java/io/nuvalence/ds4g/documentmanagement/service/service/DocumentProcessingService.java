package io.nuvalence.ds4g.documentmanagement.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.ds4g.documentmanagement.service.config.PublishDocumentProcessingConfig;
import io.nuvalence.ds4g.documentmanagement.service.config.PublishDocumentProcessingResultConfig;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorResult;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorStatus;
import io.nuvalence.ds4g.documentmanagement.service.generated.models.DocumentProcessingRequest;
import io.nuvalence.ds4g.documentmanagement.service.processor.DocumentProcessingRequestWrapper;
import io.nuvalence.ds4g.documentmanagement.service.processor.DocumentProcessor;
import io.nuvalence.ds4g.documentmanagement.service.processor.DocumentProcessorRegistry;
import io.nuvalence.ds4g.documentmanagement.service.processor.exception.RetryableDocumentProcessingException;
import io.nuvalence.ds4g.documentmanagement.service.processor.exception.UnretryableDocumentProcessingException;
import io.nuvalence.ds4g.documentmanagement.service.repository.DocumentProcessorResultRepository;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for processing documents.
 */
@Service
@Slf4j
public class DocumentProcessingService {

    private final PublishDocumentProcessingConfig.DocumentProcessingPublisher
            documentProcessingPublisher;

    private final PublishDocumentProcessingResultConfig.DocumentProcessingResultPublisher
            documentProcessingResultPublisher;

    private final DocumentProcessorRegistry documentProcessorRegistry;

    private final ObjectMapper mapper;

    private final DocumentProcessorResultRepository documentProcessorResultRepository;

    /**
     * Constructor for this service.
     *
     * @param documentProcessingPublisher PubSub publisher for processing requests.
     * @param documentProcessingResultPublisher PubSub publisher for processing results.
     * @param documentProcessorRegistry Registry of document processors.
     * @param mapper Jackson mapper.
     * @param documentProcessorResultRepository repository for document processing results.
     *
     */
    public DocumentProcessingService(
            PublishDocumentProcessingConfig.DocumentProcessingPublisher documentProcessingPublisher,
            PublishDocumentProcessingResultConfig.DocumentProcessingResultPublisher
                    documentProcessingResultPublisher,
            DocumentProcessorRegistry documentProcessorRegistry,
            ObjectMapper mapper,
            DocumentProcessorResultRepository documentProcessorResultRepository) {
        this.documentProcessingPublisher = documentProcessingPublisher;
        this.documentProcessingResultPublisher = documentProcessingResultPublisher;
        this.documentProcessorRegistry = documentProcessorRegistry;
        this.mapper = mapper;
        this.documentProcessorResultRepository = documentProcessorResultRepository;
    }

    /**
     * Creates a Document Entity from a document model.
     *
     * @param documentId ID of the document to process.
     * @param documentProcessingRequests processing request information.
     * @throws JsonProcessingException failed parsing.
     */
    public void enqueueDocumentProcessingRequest(
            String documentId, List<DocumentProcessingRequest> documentProcessingRequests)
            throws JsonProcessingException {

        for (DocumentProcessingRequest documentProcessingRequest : documentProcessingRequests) {

            documentProcessingPublisher.publish(
                    mapper.writeValueAsString(
                            DocumentProcessingRequestWrapper.builder()
                                    .request(documentProcessingRequest)
                                    .documentId(documentId)
                                    .build()));

            documentProcessorResultRepository.save(
                    DocumentProcessorResult.builder()
                            .documentId(UUID.fromString(documentId))
                            .processorId(documentProcessingRequest.getProcessorId())
                            .status(DocumentProcessorStatus.PENDING)
                            .result(Map.of())
                            .timestamp(OffsetDateTime.now())
                            .build());

            log.debug("Publish request for processing {}", documentProcessingRequest);
        }
    }

    /**
     * Syncronous method for processing a document. 
     * It either fails or completes call execution, returning a result with relevant
     * information and status (which can be used to determine if it was successful or not).
     *
     * @param documentId ID of the document to process.
     * @param documentProcessingRequest a particular request information.
     * @return DocumentProcessorResult of the processing. Never returns null.
     * @throws JsonProcessingException failed parsing.
     * @throws UnretryableDocumentProcessingException failed processing due to an unrecoverable exception.
     * @throws RetryableDocumentProcessingException failed processing due to a recoverable exception.
     */
    public @NotNull DocumentProcessorResult processRequest(
            String documentId, DocumentProcessingRequest documentProcessingRequest)
            throws JsonProcessingException, UnretryableDocumentProcessingException,
                    RetryableDocumentProcessingException {
        Optional<DocumentProcessor> processor =
                documentProcessorRegistry.getProcessor(documentProcessingRequest.getProcessorId());

        if (processor.isEmpty()) {
            var result =
                    DocumentProcessorResult.builder()
                            .documentId(UUID.fromString(documentId))
                            .processorId(documentProcessingRequest.getProcessorId())
                            .status(DocumentProcessorStatus.UNPROCESSABLE)
                            .result(Map.of("error", "processor not found"))
                            .timestamp(OffsetDateTime.now())
                            .build();
            documentProcessorResultRepository.save(result);
            log.error(
                    String.format(
                            "Processor not found %s, for document %s",
                            documentProcessingRequest.getProcessorId(), documentId));
            throw new UnretryableDocumentProcessingException("Processor not found");
        }

        var result = processor.get().process(documentId);

        // avoiding unnecessary long retry over pubsub if dependency is solved quickly
        if (result.getStatus() == DocumentProcessorStatus.MISSING_DEPENDENCY) {
            log.warn(
                    String.format(
                            "Dependency missing for processing document %s. This operation will be"
                                    + " retried",
                            documentId));
            throw new RetryableDocumentProcessingException("Missing dependency");
        }

        if (result.getStatus() == DocumentProcessorStatus.UNPROCESSABLE) {
            String unretryableErrorMessage = "An unretryable error occurred";
            if (result.getResult() instanceof Map) {
                String errorMessage = (String) ((Map) result.getResult()).get("error");
                log.error("{}: {}", unretryableErrorMessage, errorMessage);
                throw new UnretryableDocumentProcessingException(errorMessage);
            }
            log.error(unretryableErrorMessage);
            throw new UnretryableDocumentProcessingException(unretryableErrorMessage);
        }

        if (result.getStatus() != DocumentProcessorStatus.COMPLETE) {
            throw new RetryableDocumentProcessingException(
                    String.format(
                            "Document %s request could not be completed and will be retried."
                                    + " Status: %s",
                            documentId, result.getStatus()));
        }

        result.setProcessorId(processor.get().getProcessorId());

        documentProcessorResultRepository.save(result);

        documentProcessingResultPublisher.publish(mapper.writeValueAsString(result));
        log.debug(
                "Document processing result was successfully published {}",
                result.getProcessorId());

        return result;
    }

    /**
     * Retrieves a list of document processing results for a given document.
     *
     * @param documentId ID of the document whose results are to be recovered.
     * @return list of processing results.
     */
    public List<DocumentProcessorResult> getProcessingResultsForDocument(UUID documentId) {

        return documentProcessorResultRepository.findByDocumentId(documentId).stream()
                .sorted(Comparator.comparing(DocumentProcessorResult::getTimestamp))
                .collect(Collectors.toList());
    }

    public List<DocumentProcessorResult> findByDocumentIdAndListOfProcessorIds(
            UUID documentId, List<String> processorIds) {
        return documentProcessorResultRepository.findByDocumentIdAndProcessorIdIn(
                documentId, processorIds);
    }
}
