package io.nuvalence.ds4g.documentmanagement.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Response model for getting download url of a document. */
@AllArgsConstructor
@Getter
public class DocumentUrlResponse {
    /** Temporary download url for a document. */
    @JsonProperty("signed_url")
    private final String signedUrl;

    /** Seconds until the temporary url expires. */
    @JsonProperty private final int expires;
}
