package io.nuvalence.ds4g.documentmanagement.service.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** The ingestion status of an uploaded document. */
@Getter
public enum ScanStatus {
    READY(HttpStatus.OK, "Document is available for download"),
    AWAITING_SCAN(
            HttpStatus.ACCEPTED, "Document awaiting scan not yet available. Try again later."),
    FAILED_SCAN(
            HttpStatus.GONE, "Document has been permanently quarantined and cannot be retrieved.");

    private final HttpStatus status;
    private final String message;

    ScanStatus(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
