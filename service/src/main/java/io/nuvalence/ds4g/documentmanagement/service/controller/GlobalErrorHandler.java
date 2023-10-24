package io.nuvalence.ds4g.documentmanagement.service.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.nuvalence.ds4g.documentmanagement.service.model.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.util.Collections;
import java.util.List;

/**
 * Handles exceptions thrown by the controller layer.
 */
@ControllerAdvice
public class GlobalErrorHandler {

    /**
     * Error response object.
     */
    @AllArgsConstructor
    @Getter
    public static class ErrorResponse {
        private List<String> messages;

        public ErrorResponse(String message) {
            this.messages = Collections.singletonList(message);
        }
    }

    /**
     * UnsupportedMediaTypeErrorResponse object.
     */
    @AllArgsConstructor
    @Getter
    @JsonPropertyOrder({"error-code", "message"})
    private static class UnsupportedMediaTypeErrorResponse {
        @JsonProperty("error-code")
        private String errorCode;

        private String message;

        private UnsupportedMediaTypeErrorResponse(ErrorCode errorCode) {
            this.errorCode = errorCode.toString();
            this.message = errorCode.getMessage();
        }
    }

    /**
     * Return a forbidden request if a ForbiddenException is thrown.
     * @param e Forbidden exception.
     * @return Forbidden request.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleException(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Return a bad request if a UnsupportedMediaTypeStatusException is thrown.
     * @param e unsupported media type exception.
     * @return unsupported media type request.
     */
    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ResponseEntity<UnsupportedMediaTypeErrorResponse> handleMediaTypeNotSupportedException(
            UnsupportedMediaTypeStatusException e) {
        UnsupportedMediaTypeErrorResponse responseBody =
                new UnsupportedMediaTypeErrorResponse(ErrorCode.UNSUPPORTED_TYPE);

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(responseBody);
    }
}
