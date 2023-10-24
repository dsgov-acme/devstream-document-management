package io.nuvalence.ds4g.documentmanagement.service.processor.impl;

import com.google.api.gax.rpc.ApiException;
import com.google.cloud.documentai.v1beta3.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1beta3.DocumentProcessorServiceSettings;
import com.google.cloud.documentai.v1beta3.ProcessOptions;
import com.google.cloud.documentai.v1beta3.ProcessRequest;
import com.google.cloud.documentai.v1beta3.ProcessResponse;
import com.google.cloud.documentai.v1beta3.ProcessorName;
import com.google.cloud.documentai.v1beta3.RawDocument;
import com.google.protobuf.ByteString;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorResult;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorStatus;
import io.nuvalence.ds4g.documentmanagement.service.model.FileContent;
import io.nuvalence.ds4g.documentmanagement.service.model.ScanStatus;
import io.nuvalence.ds4g.documentmanagement.service.processor.DocumentProcessor;
import io.nuvalence.ds4g.documentmanagement.service.storage.StorageProvider;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract class for document processors.
 *
 * @param <R> results type
 */
@Slf4j
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public abstract class DocumentAiProcessor<R> implements DocumentProcessor {
    private static final String ENDPOINT_URL = "%s-documentai.googleapis.com:443";

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${document-management.processor.location}")
    private String location = "us";

    private String processorId;
    private DocumentProcessorServiceClient client;
    private StorageProvider storage;

    /**
     * Add Processor options.
     *
     * @return Processor Options
     */
    protected ProcessOptions addProcessorOptions() {
        return null;
    }

    /**
     * Gets the processor name.
     *
     * @return processor name
     */
    abstract String getProcessorName();

    /**
     * Parses the results of the document processor.
     *
     * @param response document processor response
     * @return parsed results
     */
    abstract R parseResults(ProcessResponse response);

    /**
     * Execute Processor, returning a DocumentProcessorResult with corresponding status. 
     * Clients of this method should retry later if the status is MISSING_DEPENDENCY since it denotes
     * an ability to be processed but waiting for a dependency to be available.
     * 
     * @param documentId document id
     * @return not null DocumentProcessorResult
     */
    public @NotNull DocumentProcessorResult process(String documentId) {

        DocumentProcessorResult.DocumentProcessorResultBuilder responseBuilder =
                DocumentProcessorResult.builder().documentId(UUID.fromString(documentId));

        try {
            var status = storage.getStatus(documentId);
            if (status == ScanStatus.AWAITING_SCAN) {
                return responseBuilder
                        .status(DocumentProcessorStatus.MISSING_DEPENDENCY)
                        .timestamp(OffsetDateTime.now())
                        .result(Map.of("state", "Waiting for processor dependency"))
                        .build();
            }

            FileContent fileData = storage.getFileData(documentId);

            String name = ProcessorName.of(projectId, location, processorId).toString();

            // Convert the image data to a Buffer and base64 encode it.
            final byte[] documentFileData = fileData.getContent().getInputStream().readAllBytes();
            ByteString content = ByteString.copyFrom(documentFileData);

            RawDocument document =
                    RawDocument.newBuilder()
                            .setContent(content)
                            .setMimeType(fileData.getContentType().toString())
                            .build();

            ProcessResponse result = client.processDocument(buildProcessRequest(name, document));

            R customParsedResult = parseResults(result);

            return responseBuilder
                    .status(DocumentProcessorStatus.COMPLETE)
                    .result(customParsedResult)
                    .timestamp(OffsetDateTime.now())
                    .build();
        } catch (ApiException e) {
            if (e.isRetryable()) {
                log.warn(
                        "Api error processing document {}, error code {} and message {}",
                        documentId,
                        e.getStatusCode().getCode(),
                        e.getMessage());
                return buildErrorProcessingResult(
                        e.getMessage(), DocumentProcessorStatus.RETRYABLE_ERROR);
            }
            log.error(
                    "Unrecoverable api error processing document {}, error code {} and message {}",
                    documentId,
                    e.getStatusCode().getCode(),
                    e.getMessage());
            return buildErrorProcessingResult(
                    e.getMessage(), DocumentProcessorStatus.UNPROCESSABLE);
        } catch (Exception e) {
            log.error("Error processing document", e);
            return buildErrorProcessingResult(
                    e.getMessage(), DocumentProcessorStatus.UNPROCESSABLE);
        }
    }

    /**
     * Sets the storage provider.
     *
     * @param storage storage provider
     */
    @Autowired
    public final void setStorage(StorageProvider storage) {
        this.storage = storage;
    }

    /**
     * Create Document AI Client.
     * @param processorId processorId
     * @throws IOException IOException
     */
    protected void createClient(String processorId) throws IOException {
        this.processorId = processorId;
        String endpoint = String.format(ENDPOINT_URL, location);
        DocumentProcessorServiceSettings settings =
                DocumentProcessorServiceSettings.newBuilder().setEndpoint(endpoint).build();
        try {
            this.client = DocumentProcessorServiceClient.create(settings);
        } catch (IOException e) {
            log.error("Error creating client", e);
        }
    }

    private ProcessRequest buildProcessRequest(String name, RawDocument document) {
        ProcessOptions processorOptions =
                addProcessorOptions() != null
                        ? addProcessorOptions()
                        : ProcessOptions.getDefaultInstance();
        return ProcessRequest.newBuilder()
                .setName(name)
                .setSkipHumanReview(true)
                .setRawDocument(document)
                .setProcessOptions(processorOptions)
                .build();
    }

    private DocumentProcessorResult buildErrorProcessingResult(
            String errorMessage, DocumentProcessorStatus status) {
        return DocumentProcessorResult.builder()
                .status(status)
                .result(Map.of("error", errorMessage))
                .timestamp(OffsetDateTime.now())
                .build();
    }

    /**
     * Close Client.
     */
    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
