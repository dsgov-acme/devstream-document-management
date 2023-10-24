package io.nuvalence.ds4g.documentmanagement.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Response model for upload requests. */
@AllArgsConstructor
@Getter
public class UploadResponse {
    @JsonProperty("document_id")
    private final String documentId;
}
