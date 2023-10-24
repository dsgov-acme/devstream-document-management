package io.nuvalence.ds4g.documentmanagement.service.processor.exception;

/**
 * Exception thrown when retryable not a document processing occurs.
 */
public class RetryableDocumentProcessingException extends Exception {

    private static final long serialVersionUID = -8941681150538717379L;

    public RetryableDocumentProcessingException(String message) {
        super(message);
    }
}
