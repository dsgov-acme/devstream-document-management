package io.nuvalence.ds4g.documentmanagement.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/** Response model for requests about document ingestion status. */
@Getter
public class ScanStatusResponse {

    /** Scan status enum. **/
    @JsonProperty("code")
    private final ScanStatus scanStatus;

    public ScanStatusResponse(ScanStatus status) {
        scanStatus = status;
    }

    /**
     * Scan status HTTP code.
     * @return integer HTTP status code (200 for OK, etc).
     */
    @JsonProperty("status")
    public int getHttpStatusCode() {
        return scanStatus.getStatus().value();
    }

    /**
     * Descriptive message for current scan status.
     * @return human-readable description of current scan status.
     */
    @JsonProperty("message")
    public String getMessage() {
        return scanStatus.getMessage();
    }
}
