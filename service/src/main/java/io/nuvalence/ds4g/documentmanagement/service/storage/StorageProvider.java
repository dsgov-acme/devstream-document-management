package io.nuvalence.ds4g.documentmanagement.service.storage;

import io.nuvalence.ds4g.documentmanagement.service.model.DocumentMetadata;
import io.nuvalence.ds4g.documentmanagement.service.model.FileContent;
import io.nuvalence.ds4g.documentmanagement.service.model.ScanStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/** Provides storage and retrieval of uploaded documents. **/
@Service
public interface StorageProvider {
    /**
     * Upload a document.
     * 
     * @param file     Document file
     * @param metadata Metadata about a document, such as type, who uploaded it,
     *                 etc.
     * @return Document ID used to retrieve uploaded document
     */
    String upload(MultipartFile file, DocumentMetadata metadata);

    /**
     * Check whether document is ready to download.
     * 
     * @param documentId Document ID, obtained during upload.
     * @return Whether the document is ready to download, scanning is incomplete, or
     *         failed.
     */
    ScanStatus getStatus(String documentId);

    /**
     * Get metadata about a document.
     * 
     * @param documentId Document ID, obtained during upload
     * @return Details about a document such as type, who uploaded it, etc.
     */
    Optional<DocumentMetadata> getMetadata(String documentId);

    /**
     * Get metadata about a document.
     * 
     * @param documentId Document ID, obtained during upload
     * @return Details about a document such as type, who uploaded it, etc.
     */
    Optional<DocumentMetadata> getUnscannedMetadata(String documentId);

    /**
     * Gets document file data.
     *
     * @param documentId Document ID, obtained during upload
     * @return Document contents of uploaded document.
     * @throws IOException if media type cannot be retrieved.
     */
    FileContent getFileData(String documentId) throws IOException;

    /**
     * Gets document file data from an unscanned bucket.
     * WARNING: This method should only be used for internal scanning flow.
     *
     * @param documentId Document ID, obtained during upload
     * @return Document contents of uploaded document.
     * @throws IOException if media type cannot be retrieved.
     */
    FileContent getUnscannedFileData(String documentId) throws IOException;

    void quaranfineFile(String documentId) throws IOException;

    void confirmCleanFile(String documentId) throws IOException;
}
