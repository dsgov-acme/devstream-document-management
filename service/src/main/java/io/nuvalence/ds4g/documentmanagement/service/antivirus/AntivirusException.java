package io.nuvalence.ds4g.documentmanagement.service.antivirus;

import java.io.IOException;

/**
 * Exception thrown when an error occurs while scanning for viruses.
 */
public class AntivirusException extends IOException {

    // Classes implementing Serializable should set a serialVersionUID
    private static final long serialVersionUID = 1L;

    public AntivirusException(String message) {
        super(message);
    }
}
