package io.nuvalence.ds4g.documentmanagement.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * Metadata about an uploaded document, such as file type.
 * Used exclusively for local development via
 * {@link io.nuvalence.ds4g.documentmanagement.service.storage.FilesystemStorage}
 */
@AllArgsConstructor
@Builder
@Getter
@Setter
public class FilesystemDocumentMetadata {
    private String originalFilename;
    private String contentType;
    private DocumentMetadata documentMetadata;

    /**
     * Metadata about an uploaded document.
     * @param file Uploaded file, used to get original file name and content type.
     * @param metadata Additional user specified metadata.
     */
    public FilesystemDocumentMetadata(MultipartFile file, DocumentMetadata metadata) {
        originalFilename = file.getOriginalFilename();
        contentType = file.getContentType();
        documentMetadata = metadata;
    }
}
