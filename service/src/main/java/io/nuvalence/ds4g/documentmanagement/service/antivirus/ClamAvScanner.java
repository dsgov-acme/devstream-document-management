package io.nuvalence.ds4g.documentmanagement.service.antivirus;

import autovalue.shaded.org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.ClamavException;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * Antivirus provider that uses ClamAV to scan documents.
 */
@ConditionalOnProperty(name = "document-management.clamav.enabled", havingValue = "true")
@Service
public class ClamAvScanner implements AntivirusProvider {

    ClamavClient client;

    public ClamAvScanner(@Value("${document-management.clamav.host:localhost}") String clamavHost) {
        this.client = new ClamavClient(clamavHost);
    }

    @Override
    public GeneralScanResult scan(@NotNull InputStream dataStream, @NotNull String documentId)
            throws AntivirusException {
        try {
            ScanResult scanResult = client.scan(dataStream);

            if (scanResult instanceof ScanResult.OK) {
                return new GeneralScanResult(true, "Clean");
            } else if (scanResult instanceof ScanResult.VirusFound) {
                Map<String, Collection<String>> viruses =
                        ((ScanResult.VirusFound) scanResult).getFoundViruses();
                return new GeneralScanResult(false, viruses.get("stream").toString());
            }
            throw new AntivirusException("Unknown scan result: " + scanResult.getClass().getName());
        } catch (ClamavException e) {
            throw new AntivirusException(e.getMessage());
        }
    }
}
