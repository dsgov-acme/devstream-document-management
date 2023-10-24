package io.nuvalence.ds4g.documentmanagement.service.antivirus;

import autovalue.shaded.org.jetbrains.annotations.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Antivirus provider that fakes scanning behavior when clamav is disabled.
 */
@ConditionalOnMissingBean(ClamAvScanner.class)
@Service
@Slf4j
public class FakeAvScanner implements AntivirusProvider {

    private static final String EICAR_TEST_STRING = "EICAR-STANDARD-ANTIVIRUS-TEST-FILE";

    @Override
    public GeneralScanResult scan(@NotNull InputStream dataStream, @NotNull String documentId)
            throws AntivirusException {

        log.warn(
                "Real malware scanner is disabled. Using fake scanner for document {}", documentId);

        String str = inputStreamToString(dataStream);
        if (str.contains(EICAR_TEST_STRING)) {
            return new GeneralScanResult(false, EICAR_TEST_STRING);
        }

        return new GeneralScanResult(true, "Clean");
    }

    private String inputStreamToString(InputStream is) {
        Scanner scanner = new Scanner(is, StandardCharsets.UTF_8);
        // read everything after the beginning of all the input.
        String text = scanner.useDelimiter("\\A").next();
        scanner.close();
        return text;
    }
}
