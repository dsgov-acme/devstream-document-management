package io.nuvalence.ds4g.documentmanagement.service.processor.exception;

/**
 * Exception thrown when retryable a document processing occurs.
 */
public class UnretryableDocumentProcessingException extends Exception {

    private static final long serialVersionUID = 2588759458554505008L;

    public UnretryableDocumentProcessingException(String message) {
        super(message);
    }
}
