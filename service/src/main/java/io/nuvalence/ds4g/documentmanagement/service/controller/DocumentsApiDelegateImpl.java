package io.nuvalence.ds4g.documentmanagement.service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorResult;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorStatus;
import io.nuvalence.ds4g.documentmanagement.service.generated.controllers.DocumentsApiDelegate;
import io.nuvalence.ds4g.documentmanagement.service.generated.models.DocumentProcessingRequest;
import io.nuvalence.ds4g.documentmanagement.service.generated.models.DocumentProcessingResultModel;
import io.nuvalence.ds4g.documentmanagement.service.generated.models.GetDocumentResponse;
import io.nuvalence.ds4g.documentmanagement.service.generated.models.UploadResponse;
import io.nuvalence.ds4g.documentmanagement.service.mapper.DocumentProcessorResultMapper;
import io.nuvalence.ds4g.documentmanagement.service.model.DocumentMetadata;
import io.nuvalence.ds4g.documentmanagement.service.model.FileContent;
import io.nuvalence.ds4g.documentmanagement.service.model.ScanStatus;
import io.nuvalence.ds4g.documentmanagement.service.model.ScanStatusResponse;
import io.nuvalence.ds4g.documentmanagement.service.service.DocumentProcessingService;
import io.nuvalence.ds4g.documentmanagement.service.service.DocumentService;
import io.nuvalence.ds4g.documentmanagement.service.storage.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of DocumentsApiDelegate.
 */
@Service
@Slf4j
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
public class DocumentsApiDelegateImpl implements DocumentsApiDelegate {
    private static final String DOCUMENT_NOT_FOUND_MESSAGE = "Document not found.";

    private final StorageProvider storage;
    private final AuthorizationHandler authorizationHandler;

    private final DocumentService documentService;

    private final DocumentProcessingService documentProcessingService;

    @Value("${allowed-types.mime-types}")
    private final ArrayList<String> allowedMimeTypes;

    @Value("${allowed-types.octet-stream-extensions}")
    private final ArrayList<String> allowedOctetExtension;

    /**
     * Constructor.
     *
     * @param storage               Storage Provider
     * @param authorizationHandler  Authorization Handler
     * @param allowedMimeTypes      allowed mime type list used during initialization.
     * @param allowedOctetExtension allowed octet-stream list used during initialization.
     * @param documentService service for manipulation Document object.
     * @param documentProcessingService documentService service for processing objects.
     */
    public DocumentsApiDelegateImpl(
            StorageProvider storage,
            AuthorizationHandler authorizationHandler,
            List<String> allowedMimeTypes,
            List<String> allowedOctetExtension,
            DocumentService documentService,
            DocumentProcessingService documentProcessingService) {
        this.storage = storage;
        this.authorizationHandler = authorizationHandler;
        this.allowedMimeTypes = (ArrayList<String>) allowedMimeTypes;
        this.allowedOctetExtension = (ArrayList<String>) allowedOctetExtension;
        this.documentService = documentService;
        this.documentProcessingService = documentProcessingService;
    }

    /**
     * Handles the posting of file uploads.
     * @param file  (optional)
     * @return File id
     */
    @Override
    public ResponseEntity<UploadResponse> upload(MultipartFile file) {
        if (!authorizationHandler.isAllowed("create", FileContent.class)) {
            throw new AccessDeniedException("You do not have permission to create this resource.");
        }

        Tika tika = new Tika();

        try {
            String filetype = tika.detect(file.getInputStream());

            if (Objects.equals(file.getContentType(), "application/octet-stream")
                    && !allowedOctetExtension.contains(filetype)) {
                allowedOctetExtension.forEach(log::info);
                log.warn(
                        String.format(
                                "octet-stream extensions not allowed: '%s'; filename: '%s'",
                                file.getContentType(), file.getOriginalFilename()));
                throw new UnsupportedMediaTypeStatusException("File type not allowed.");
            }

            if (!allowedMimeTypes.toString().contains(filetype)) {
                allowedMimeTypes.forEach(log::info);
                log.warn(
                        String.format(
                                "Mimetype not allowed: '%s'; filename: '%s'",
                                file.getContentType(), file.getOriginalFilename()));
                throw new UnsupportedMediaTypeStatusException("File type not allowed.");
            }
        } catch (IOException e) {
            log.error("Error reading file", e);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String uploadedUserId;
        if (authentication instanceof UserToken) {
            uploadedUserId = (String) authentication.getPrincipal();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User token not found");
        }

        DocumentMetadata metadata =
                DocumentMetadata.builder()
                        .originalFilename(file.getOriginalFilename())
                        .uploadedBy(uploadedUserId)
                        .build();

        String documentId = storage.upload(file, metadata);

        documentService.createDocument(documentId, file.getOriginalFilename(), uploadedUserId);

        return ResponseEntity.accepted()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UploadResponse().documentId(documentId));
    }

    /**
     * Get Document metadata.
     * @param documentId Document id
     * @return GetDocumentResponse object
     */
    @Override
    public ResponseEntity<GetDocumentResponse> getDocument(String documentId) {
        DocumentMetadata documentMetadata =
                storage.getMetadata(documentId)
                        .filter(dm -> authorizationHandler.isAllowedForInstance("view", dm))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, DOCUMENT_NOT_FOUND_MESSAGE));

        GetDocumentResponse documentResponse =
                new GetDocumentResponse()
                        .id(UUID.fromString(documentId))
                        .filename(documentMetadata.getOriginalFilename())
                        .id(UUID.fromString(documentId))
                        .uploadedBy(documentMetadata.getUploadedBy());

        return ResponseEntity.status(HttpStatus.OK).body(documentResponse);
    }

    /**
     * Handles downloading of a file.
     * @param documentId Document id
     * @return File content (binary data)
     */
    @Override
    public ResponseEntity<byte[]> getFileData(String documentId) {
        DocumentMetadata documentMetadata =
                storage.getMetadata(documentId)
                        .filter(dm -> authorizationHandler.isAllowedForInstance("view", dm))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, DOCUMENT_NOT_FOUND_MESSAGE));
        log.trace(
                "The metadata for document {} with id {} was retrieved.",
                documentMetadata.getOriginalFilename(),
                documentId);

        FileContent download;
        byte[] fileContentInBytes;
        try {
            download = storage.getFileData(documentId);
            fileContentInBytes = IOUtils.toByteArray(download.getContent().getInputStream());
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Error getting document");
        }

        return ResponseEntity.ok().contentType(download.getContentType()).body(fileContentInBytes);
    }

    @Override
    public ResponseEntity<Void> enqueueDocumentProcessingRequest(
            String documentId,
            List<DocumentProcessingRequest> documentProcessingRequests,
            Boolean reprocess) {

        if (!authorizationHandler.isAllowed("create", DocumentProcessorResult.class)) {
            throw new AccessDeniedException("You do not have permission to create this resource.");
        }

        if (documentProcessingRequests.isEmpty()) {
            log.error("No processing data was provided");
            return ResponseEntity.badRequest().build();
        }

        documentProcessingRequests =
                handleReprocessing(documentId, documentProcessingRequests, reprocess);

        if (!documentProcessingRequests.isEmpty()) {
            ScanStatusResponse status = new ScanStatusResponse(storage.getStatus(documentId));

            if (status.getScanStatus().equals(ScanStatus.FAILED_SCAN)) {
                return ResponseEntity.status(HttpStatus.GONE).build();
            }

            log.debug("Enqueuing processing request for document with id {}", documentId);

            try {
                documentProcessingService.enqueueDocumentProcessingRequest(
                        documentId, documentProcessingRequests);
            } catch (JsonProcessingException e) {
                log.error("The message with id {} could not be enqueued", documentId);
                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.ok().build();
    }

    private List<DocumentProcessingRequest> handleReprocessing(
            String documentId,
            List<DocumentProcessingRequest> documentProcessingRequests,
            boolean reprocess) {

        if (!reprocess) {
            List<DocumentProcessorResult> existingProcessingResults =
                    documentProcessingService.findByDocumentIdAndListOfProcessorIds(
                            UUID.fromString(documentId),
                            documentProcessingRequests.stream()
                                    .map(DocumentProcessingRequest::getProcessorId)
                                    .collect(Collectors.toList()));

            List<DocumentProcessingRequest> filteredDocumentProcessingRequests =
                    documentProcessingRequests.stream()
                            .filter(
                                    request ->
                                            existingProcessingResults.stream()
                                                    .noneMatch(
                                                            result ->
                                                                    result.getProcessorId()
                                                                            .equals(
                                                                                    request
                                                                                            .getProcessorId())))
                            .collect(Collectors.toList());

            if (!existingProcessingResults.isEmpty()) {
                log.info(
                        "Document {} already processed by the following processors: {}, request"
                                + " discarded for them",
                        documentId,
                        existingProcessingResults.stream()
                                .map(DocumentProcessorResult::getProcessorId)
                                .collect(Collectors.toList()));
            }

            return filteredDocumentProcessingRequests;
        }
        return documentProcessingRequests;
    }

    @Override
    public ResponseEntity<List<DocumentProcessingResultModel>> getDocumentProcessingResults(
            String id) {

        var status = storage.getStatus(id);
        Optional<DocumentMetadata> dm = Optional.empty();
        switch (status) {
            case READY:
                dm = storage.getMetadata(id);
                break;
            case AWAITING_SCAN:
                dm = storage.getUnscannedMetadata(id);
                break;
            default:
                break;
        }

        if (dm.isEmpty() || !authorizationHandler.isAllowedForInstance("view", dm.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, DOCUMENT_NOT_FOUND_MESSAGE);
        }

        // initial Approach to fake the Antivirus Scan Result processor
        ScanStatusResponse scanStatusResponse = new ScanStatusResponse(status);

        DocumentProcessingResultModel antivirusProcessorResult =
                new DocumentProcessingResultModel();
        antivirusProcessorResult.setProcessorId("antivirus-scanner");
        antivirusProcessorResult.setResult(scanStatusResponse);
        antivirusProcessorResult.setTimestamp(String.valueOf(OffsetDateTime.now()));
        antivirusProcessorResult.setStatus(DocumentProcessorStatus.COMPLETE.name());

        if (scanStatusResponse
                .getScanStatus()
                .getStatus()
                .equals(ScanStatus.AWAITING_SCAN.getStatus())) {
            antivirusProcessorResult.setStatus(DocumentProcessorStatus.PENDING.name());
        }

        List<DocumentProcessingResultModel> documentProcessingResultModels =
                documentProcessingService
                        .getProcessingResultsForDocument(UUID.fromString(id))
                        .stream()
                        .map(DocumentProcessorResultMapper.INSTANCE::toModel)
                        .collect(Collectors.toList());

        documentProcessingResultModels.add(antivirusProcessorResult);

        return ResponseEntity.status(200).body(documentProcessingResultModels);
    }
}
