package io.nuvalence.ds4g.documentmanagement.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.nuvalence.auth.access.AccessResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** Response model for getting all details about an uploaded documents. **/
@AllArgsConstructor
@Builder
@Getter
@AccessResource("document")
public class GetDocumentResponse {
    @JsonProperty private final UUID id;

    @JsonProperty private final String scanStatus;

    @JsonProperty private final String originalFilename;

    @JsonProperty private final String uploadedBy;
}
