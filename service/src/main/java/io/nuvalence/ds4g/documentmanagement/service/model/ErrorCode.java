package io.nuvalence.ds4g.documentmanagement.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** List of custom error codes. */
@AllArgsConstructor
@Getter
public enum ErrorCode {
    UNSUPPORTED_TYPE("File type not allowed.");

    private final String message;
}
