package io.nuvalence.ds4g.documentmanagement.service.service;

import io.nuvalence.ds4g.documentmanagement.service.entity.Document;
import io.nuvalence.ds4g.documentmanagement.service.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Document.
 */
@Service
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;

    /**
     * Constructor for this service.
     *
     * @param documentRepository Repository interface for documents.
     *
     */
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Creates a Document Entity from a document model.
     *
     * @param documentId ID of the document whose information is to be persisted.
     * @param fileName original file name.
     * @param uploaderId id of the uploader.
     * @return the new Document Entity
     */
    public Document createDocument(String documentId, String fileName, String uploaderId) {

        Document document =
                Document.builder()
                        .id(UUID.fromString(documentId))
                        .filename(fileName)
                        .processingResults(new ArrayList<>())
                        .uploadedBy(uploaderId)
                        .build();

        return documentRepository.save(document);
    }

    /**
     * Returns a Document based on the document's id.
     *
     * @param documentId is the document's id.
     * @return a Document.
     */
    public Optional<Document> getUDocumentById(UUID documentId) {
        return documentRepository.findById(documentId);
    }
}
