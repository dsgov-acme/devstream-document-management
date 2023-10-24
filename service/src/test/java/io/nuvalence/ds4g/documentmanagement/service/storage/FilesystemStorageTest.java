package io.nuvalence.ds4g.documentmanagement.service.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nuvalence.ds4g.documentmanagement.service.model.DocumentMetadata;
import io.nuvalence.ds4g.documentmanagement.service.model.FileContent;
import io.nuvalence.ds4g.documentmanagement.service.model.ScanStatus;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

class FilesystemStorageTest {
    FilesystemStorage storage;

    /** Mock a file that will pass scans and be available for download. */
    MockMultipartFile file;

    /** Mock a file that will stay in the unscanned state, and not be available for download. */
    MockMultipartFile unscannedFile;

    /** Mock a file that will fail scanning, and be quarantined. */
    MockMultipartFile quarantineFile;

    DocumentMetadata metadata;

    private final String uploadedBy = "string_id";

    @AfterEach()
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File("./data"));
    }

    @BeforeEach
    void setup() throws Exception {
        storage = new FilesystemStorage();
        file =
                new MockMultipartFile(
                        "file",
                        "hello.txt",
                        MediaType.TEXT_PLAIN_VALUE,
                        "I am a squeaky clean file :)".getBytes(StandardCharsets.UTF_8));

        unscannedFile =
                new MockMultipartFile(
                        "file",
                        "WAIT.txt",
                        MediaType.TEXT_PLAIN_VALUE,
                        "I am a file is waiting to be scanned (-.-)Zzz"
                                .getBytes(StandardCharsets.UTF_8));

        quarantineFile =
                new MockMultipartFile(
                        "file",
                        "QUARANTINE.txt",
                        MediaType.TEXT_PLAIN_VALUE,
                        "I am a file that should be quarantined >:("
                                .getBytes(StandardCharsets.UTF_8));

        metadata = new DocumentMetadata(uploadedBy, "some_file.txt");
    }

    @Test
    void upload() {
        String documentId = storage.upload(file, metadata);
        assertNotNull(documentId);
    }

    @Test
    void uploadIoExceptionRethrownAsStatusException() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("hello.txt");
        when(mockFile.getBytes()).thenThrow(IOException.class);

        assertThrows(ResponseStatusException.class, () -> storage.upload(mockFile, metadata));
    }

    @Test
    void statusOfDocumentNotFound() {
        assertThrowsWithMessage(() -> storage.getStatus(UUID.randomUUID().toString()));
    }

    private void assertThrowsWithMessage(Executable executable) {
        Throwable thrownException =
                assertThrows(
                        (Class<? extends Throwable>) ResponseStatusException.class, executable);
        assertEquals(
                HttpStatus.NOT_FOUND, ((ResponseStatusException) thrownException).getStatusCode());
    }

    @Test
    void statusOfDocumentOk() {
        String documentId = storage.upload(file, metadata);
        ScanStatus status = storage.getStatus(documentId);
        assertEquals(HttpStatus.OK, status.getStatus());
    }

    @Test
    void statusOfDocumentUnscanned() {
        String documentId = storage.upload(unscannedFile, metadata);
        ScanStatus status = storage.getStatus(documentId);
        assertEquals(HttpStatus.ACCEPTED, status.getStatus());
    }

    @Test
    void statusOfDocumentFailedScan() {
        String documentId = storage.upload(quarantineFile, metadata);
        ScanStatus status = storage.getStatus(documentId);
        assertEquals(HttpStatus.GONE, status.getStatus());
    }

    @Test
    void getMetadata() {
        String documentId = storage.upload(file, metadata);
        DocumentMetadata result = storage.getMetadata(documentId).get();
        assertEquals(metadata, result);
    }

    @Test
    void getFileData() throws IOException {
        final String documentId = storage.upload(file, metadata);
        final MediaType expectedMediaType = MediaType.TEXT_PLAIN;
        FileContent fileContent = storage.getFileData(documentId);
        ByteArrayResource byteArrayResource = (ByteArrayResource) fileContent.getContent();
        assertEquals(expectedMediaType, fileContent.getContentType());
        assertArrayEquals(file.getBytes(), byteArrayResource.getByteArray());
    }

    @Test
    void getFileDataWithNonExistingDocument() {
        assertThrows(ResponseStatusException.class, () -> storage.getFileData("nonExistingFile"));
    }

    @Test
    void getFileDataWithNonExistingMetadata() {
        final String documentId = storage.upload(file, null);
        assertThrows(ResponseStatusException.class, () -> storage.getFileData(documentId));
    }

    @Test
    void getUnscannedFileData() throws IOException {
        final String documentId = storage.upload(unscannedFile, metadata);
        final MediaType expectedMediaType = MediaType.TEXT_PLAIN;
        FileContent fileContent = storage.getUnscannedFileData(documentId);
        ByteArrayResource byteArrayResource = (ByteArrayResource) fileContent.getContent();
        assertEquals(expectedMediaType, fileContent.getContentType());
        assertArrayEquals(unscannedFile.getBytes(), byteArrayResource.getByteArray());
    }

    @Test
    void getMetadataDocumentNotFound() {
        String documentId = UUID.randomUUID().toString();
        assertThrows(ResponseStatusException.class, () -> storage.getMetadata(documentId));
    }

    @Test
    void getUnscannedMetaData() {
        String documentId = UUID.randomUUID().toString();
        assertThrows(
                UnsupportedOperationException.class,
                () -> storage.getUnscannedMetadata(documentId));
    }

    @Test
    void quaranfineFile() {
        String documentId = UUID.randomUUID().toString();
        assertThrows(UnsupportedOperationException.class, () -> storage.quaranfineFile(documentId));
    }

    @Test
    void confirmCleanFile() {
        String documentId = UUID.randomUUID().toString();
        assertThrows(
                UnsupportedOperationException.class, () -> storage.confirmCleanFile(documentId));
    }

    @Test
    void getDataFromFileMetadaDataNotFound() {
        String documentId = UUID.randomUUID().toString();
        // when(storage.getMetadata(documentId)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> storage.getMetadata(documentId));
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    @Test
    void testSetFilePermissions()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException,
                    IOException {
        // Call the method you want to test use relflection only for testing
        File testFile = Files.createTempDirectory("uploads").toFile();
        Method method = FilesystemStorage.class.getDeclaredMethod("setFilePermissions", File.class);
        method.setAccessible(true);
        method.invoke(storage, testFile);

        // Verify that the setReadable, setWritable, and setExecutable methods were called
        assertTrue(testFile.canRead(), "File should be readable");
        assertTrue(testFile.canWrite(), "File should be writable");
        assertTrue(testFile.canExecute(), "File should be executable");
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    @Test
    void testErrorSettingFilePermissions() throws NoSuchMethodException {
        // Call the method you want to test use relflection only for testing
        File testFile = new File("uploads");
        Method method = FilesystemStorage.class.getDeclaredMethod("setFilePermissions", File.class);
        method.setAccessible(true);
        assertThrows(InvocationTargetException.class, () -> method.invoke(storage, testFile));
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    @Test
    void testCreateDirectory()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException,
                    IOException {
        // Create a temporary test directory
        File testDirectory = Files.createTempDirectory("testDir").toFile();
        File testFile = new File(testDirectory, "testFile");
        // Call the method you want to test using reflection
        Method method = FilesystemStorage.class.getDeclaredMethod("createDirectory", File.class);
        method.setAccessible(true);
        method.invoke(storage, testFile);
        // Verify that the directory was created
        assertTrue(testDirectory.exists(), "Directory should exist");
        // Clean up: delete the test directory
        testDirectory.delete();
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    @Test
    void testErrorCreatingDirectory() throws NoSuchMethodException, IOException {
        // Create a temporary test directory
        File testDirectory = Files.createTempDirectory("testDir").toFile();
        // Call the method you want to test using reflection
        Method method = FilesystemStorage.class.getDeclaredMethod("createDirectory", File.class);
        method.setAccessible(true);
        // Verify that the directory was created
        assertThrows(InvocationTargetException.class, () -> method.invoke(storage, testDirectory));
    }
}
