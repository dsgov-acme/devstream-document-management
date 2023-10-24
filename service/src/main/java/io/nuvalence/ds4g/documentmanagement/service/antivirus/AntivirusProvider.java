package io.nuvalence.ds4g.documentmanagement.service.antivirus;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.InputStream;

/** Antivirus provider that can scan documents for viruses. */
public interface AntivirusProvider {

    /**
     * Represents the result of a virus scan operation.
     */
    @Getter
    @AllArgsConstructor
    class GeneralScanResult {
        /**
         * Flag indicating if the scanned data was clean (free of viruses).
         */
        private final boolean clean;
        /**
         * Flag indicating if the scanned data was clean (free of viruses).
         */
        private final String message;
    }

    /**
     * Scans the provided data stream for viruses.
     *
     * @param dataStream The InputStream representing the data to be scanned.
     * @param documentId The identifier of the document being scanned.
     * @return The result of the scan operation or null if the scan operation failed.
     * @throws AntivirusException If the scan operation failed.
     */
    GeneralScanResult scan(InputStream dataStream, String documentId) throws AntivirusException;
}
