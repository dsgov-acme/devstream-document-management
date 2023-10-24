package io.nuvalence.ds4g.documentmanagement.service.model;

import static io.nuvalence.ds4g.documentmanagement.service.model.DocumentMetadataKey.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.storage.Blob;
import io.nuvalence.auth.access.AccessResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Metadata for an uploaded document, like document category and user-added descriptions.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@AccessResource("document")
public class DocumentMetadata {

    /**
     * ID of the user that uploaded a document.
     */
    @JsonProperty private String uploadedBy;

    /**
     * Original file name of uploaded document.
     */
    @JsonProperty private String originalFilename;

    /**
     * Create a new object from google cloud storage metadata, obtained via {@link Blob#getMetadata()}.
     * @param map Google cloud storage object metadata.
     */
    public DocumentMetadata(Map<String, String> map) {
        uploadedBy = map.get(UPLOADED_BY.gcpKey);
        originalFilename = map.get(ORIGINAL_FILENAME.gcpKey);
    }

    /**
     * Gets document metadata in format used by cloud storage library.
     * @return document metadata as a map of cloud storage key-value pairs.
     */
    public Map<String, String> toCloudStorageMetadataFormat() {
        Map<String, String> map = new HashMap<>();
        map.put(UPLOADED_BY.gcpKey, uploadedBy);
        map.put(ORIGINAL_FILENAME.gcpKey, originalFilename);

        return map;
    }
}
