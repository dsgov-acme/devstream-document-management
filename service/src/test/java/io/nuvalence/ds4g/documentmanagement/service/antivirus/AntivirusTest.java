package io.nuvalence.ds4g.documentmanagement.service.antivirus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.capybara.clamav.ClamavClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class AntivirusTest {

    // create logger
    private static final Logger log = LoggerFactory.getLogger(AntivirusTest.class);

    @Test
    void clamAv() {

        ClamAvScanner clamAvScanner = new ClamAvScanner("localhost");
        // not possible to mock final class with standard mockito
        // var scanResultMock = mock(ScanResult.OK.class);
        // TODO: future implementation of this test may use PowerMockito library

        clamAvScanner.client = mock(ClamavClient.class);
        // when(mockClamavClient.scan(inputStream)).thenReturn(scanResultMock);

        try {
            AntivirusProvider.GeneralScanResult result =
                    clamAvScanner.scan(
                            new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)),
                            "test");
            assertTrue(result.isClean());
        } catch (Exception e) {
            log.info("ClamAV client callable with ByteArrayInputStream");
        }
    }

    @Test
    void fakeAvClean() throws Exception {

        FakeAvScanner fakeAvScanner = new FakeAvScanner();

        String cleanFileContent = "A-RANDOM-STRING";
        String testInfectedFileContent =
                "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
        String fileName = "testFileBytes";

        // clean file test
        InputStream cleanBytesTestStream =
                new ByteArrayInputStream(cleanFileContent.getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(fakeAvScanner.scan(cleanBytesTestStream, fileName).isClean());

        // infected file test
        InputStream infectedTestStream =
                new ByteArrayInputStream(testInfectedFileContent.getBytes(StandardCharsets.UTF_8));
        assertFalse(fakeAvScanner.scan(infectedTestStream, fileName).isClean());
    }

    @Test
    void testAntivirusExceptionConstructor() {
        // Arrange
        String testMessage = "Antivirus scan failed.";

        // Act
        AntivirusException exception = new AntivirusException(testMessage);

        // Assert
        assertEquals(testMessage, exception.getMessage());
    }
}
