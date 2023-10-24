package io.nuvalence.ds4g.documentmanagement.service.util;

import static io.nuvalence.ds4g.documentmanagement.service.util.FileUtils.getByteArrayResourceFromFile;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

class FileUtilsTest {

    private static final String FILE_CONTENT = "This is a test file.";

    @Test
    void getMediaTypeOfFile() {
        final String fileName = "testFile.pdf";
        final MediaType expectedMediaType = MediaType.APPLICATION_PDF;
        MediaType actualMediaType;

        try {
            actualMediaType = FileUtils.getMediaTypeOfFile(new File(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assertEquals(expectedMediaType, actualMediaType);
    }

    @Test
    void getMediaTypeOfFile_throwsExceptionWithInvalidFileName() {
        final String fileName = "testFile.1234";
        assertThrowsWithMessage(
                () -> {
                    FileUtils.getMediaTypeOfFile(new File(fileName));
                });
    }

    private void assertThrowsWithMessage(Executable executable) {
        Throwable thrownException =
                assertThrows(
                        (Class<? extends Throwable>) ResponseStatusException.class, executable);
        assertNotNull(thrownException.getMessage());
        assertTrue(thrownException.getMessage().contains("File type not found."));
    }

    @Test
    void getByteArrayResourceFromFile_InvalidFile_ThrowsRuntimeException() {
        // Arrange
        File invalidFile = new File("nonexistent.txt");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> getByteArrayResourceFromFile(invalidFile));
    }

    @Test
    void getByteArrayResourceFromFile_ValidFile_ReturnsByteArrayResource() throws IOException {
        // Arrange
        File file = createTestFile();

        // Act
        ByteArrayResource resource = getByteArrayResourceFromFile(file);

        // Assert
        assertNotNull(resource);
        assertArrayEquals(FILE_CONTENT.getBytes(StandardCharsets.UTF_8), resource.getByteArray());

        // Clean up
        deleteTestFile(file);
    }

    @Test
    void testGetByteArrayResourceFromEmptyFile() throws IOException {
        File file = File.createTempFile("test", ".txt");
        assertThrows(ResponseStatusException.class, () -> getByteArrayResourceFromFile(file));
    }

    private File createTestFile() throws IOException {
        File file = File.createTempFile("test", ".txt");
        Charset charset = StandardCharsets.UTF_8; // Specify the appropriate charset

        try (FileWriter writer = new FileWriter(file, charset)) {
            writer.write(FileUtilsTest.FILE_CONTENT);
        }

        return file;
    }

    private void deleteTestFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }
}
