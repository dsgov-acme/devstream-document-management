package io.nuvalence.ds4g.documentmanagement.service.storage;

import static io.nuvalence.ds4g.documentmanagement.service.model.DocumentMetadataKey.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import io.nuvalence.ds4g.documentmanagement.service.model.DocumentMetadata;
import io.nuvalence.ds4g.documentmanagement.service.model.FileContent;
import io.nuvalence.ds4g.documentmanagement.service.model.ScanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class GoogleCloudStorageTest {
    Storage mockGoogleCloudStorageLibrary;
    GoogleCloudStorage storageProvider;
    MultipartFile file;
    DocumentMetadata documentMetadata = new DocumentMetadata();
    String documentId = "1381FA0A-45CF-4B91-AF2C-47C7A81B03E1";
    int signedUrlDurationSeconds = 1;
    private final String userId = "string_id";
    private final String originalFilename = "hello.txt";
    private final String originalFileContents = "Hello World!";

    @BeforeEach
    void setup() {
        mockGoogleCloudStorageLibrary = Mockito.mock(Storage.class);
        storageProvider =
                new GoogleCloudStorage(
                        "unscanned", "quarantine", "scanned", mockGoogleCloudStorageLibrary);

        file =
                new MockMultipartFile(
                        "file",
                        originalFilename,
                        MediaType.TEXT_PLAIN_VALUE,
                        originalFileContents.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void quarantine() throws Exception {

        Blob mockBlob = mock(Blob.class);
        Blob mockBlobNew = mock(Blob.class);
        BlobId unscannedObject = BlobId.of("unscanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(unscannedObject)).thenReturn(mockBlob);
        CopyWriter mockCopyWriter = mock(CopyWriter.class);
        when(mockBlob.copyTo("quarantine", documentId)).thenReturn(mockCopyWriter);
        when(mockCopyWriter.getResult()).thenReturn(mockBlobNew);
        when(mockBlobNew.exists()).thenReturn(true);
        when(mockBlob.delete()).thenReturn(true);

        storageProvider.quaranfineFile(documentId);

        // verifications
        verify(mockBlob).copyTo("quarantine", documentId);
        verify(mockBlobNew).exists();
        verify(mockBlob).delete();
    }

    @Test
    void confirmCleanFile() throws Exception {

        Blob mockBlob = mock(Blob.class);
        Blob mockBlobNew = mock(Blob.class);
        BlobId unscannedObject = BlobId.of("unscanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(unscannedObject)).thenReturn(mockBlob);
        CopyWriter mockCopyWriter = mock(CopyWriter.class);
        when(mockBlob.copyTo("scanned", documentId)).thenReturn(mockCopyWriter);
        when(mockCopyWriter.getResult()).thenReturn(mockBlobNew);
        when(mockBlobNew.exists()).thenReturn(true);
        when(mockBlob.delete()).thenReturn(true);

        storageProvider.confirmCleanFile(documentId);

        // verifications
        verify(mockBlob).copyTo("scanned", documentId);
        verify(mockBlobNew).exists();
        verify(mockBlob).delete();
    }

    @Test
    void failWhenUnableToCopy() {

        Blob mockBlob = mock(Blob.class);
        Blob mockBlobNew = mock(Blob.class);
        BlobId unscannedObject = BlobId.of("unscanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(unscannedObject)).thenReturn(mockBlob);
        CopyWriter mockCopyWriter = mock(CopyWriter.class);
        when(mockBlob.copyTo("quarantine", documentId)).thenReturn(mockCopyWriter);
        when(mockCopyWriter.getResult()).thenReturn(mockBlobNew);
        when(mockBlobNew.exists()).thenReturn(false);

        // verifications
        assertThrows(IOException.class, () -> storageProvider.quaranfineFile(documentId));
        verify(mockBlob).copyTo("quarantine", documentId);
        verify(mockBlobNew).exists();
    }

    @Test
    void failWhenUnableToDeleteSource() {

        Blob mockBlob = mock(Blob.class);
        Blob mockBlobNew = mock(Blob.class);
        BlobId unscannedObject = BlobId.of("unscanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(unscannedObject)).thenReturn(mockBlob);
        CopyWriter mockCopyWriter = mock(CopyWriter.class);
        when(mockBlob.copyTo("scanned", documentId)).thenReturn(mockCopyWriter);
        when(mockCopyWriter.getResult()).thenReturn(mockBlobNew);
        when(mockBlobNew.exists()).thenReturn(true);
        when(mockBlob.delete()).thenReturn(false);

        // verifications
        assertThrows(IOException.class, () -> storageProvider.confirmCleanFile(documentId));
        verify(mockBlob).copyTo("scanned", documentId);
        verify(mockBlobNew).exists();
        verify(mockBlob).delete();
    }

    @Test
    void upload() throws Exception {
        documentMetadata.setUploadedBy("string_id");
        String documentId = storageProvider.upload(file, documentMetadata);

        assertNotNull(documentId);
        ArgumentCaptor<BlobInfo> blobInfo = ArgumentCaptor.forClass(BlobInfo.class);
        verify(mockGoogleCloudStorageLibrary).create(blobInfo.capture(), eq(file.getBytes()));
        assertEquals(
                file.getContentType(),
                blobInfo.getValue().getContentType(),
                "Content-Type must be set when object is created");
    }

    @Test
    void uploadFailed() {
        documentMetadata.setUploadedBy("string_id");
        doThrow(new StorageException(500, "Internal Server Error"))
                .when(mockGoogleCloudStorageLibrary)
                .create(any(BlobInfo.class), any(byte[].class));

        assertThrows(
                ResponseStatusException.class,
                () -> storageProvider.upload(file, documentMetadata),
                "Failed to upload file to google cloud");
    }

    @Test
    void uploadWithMetadata() throws Exception {
        DocumentMetadata requestMetadata =
                DocumentMetadata.builder()
                        .uploadedBy(userId)
                        .originalFilename(originalFilename)
                        .build();

        storageProvider.upload(file, requestMetadata);

        ArgumentCaptor<BlobInfo> blobInfo = ArgumentCaptor.forClass(BlobInfo.class);
        verify(mockGoogleCloudStorageLibrary).create(blobInfo.capture(), eq(file.getBytes()));
        Map<String, String> actualObjectMetadata = blobInfo.getValue().getMetadata();
        assertEquals(userId, actualObjectMetadata.get(UPLOADED_BY.gcpKey));
        assertEquals(originalFilename, actualObjectMetadata.get(ORIGINAL_FILENAME.gcpKey));
    }

    @Test
    void statusAwaitingScan() {
        // Setup
        Blob mockBlob = mock(Blob.class);
        BlobId unscannedObject = BlobId.of("unscanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(unscannedObject)).thenReturn(mockBlob);

        // Execute
        ScanStatus status = storageProvider.getStatus(documentId);

        // Verify
        assertEquals(ScanStatus.AWAITING_SCAN, status);
    }

    @Test
    void statusFailedScan() {
        // Setup
        Blob mockBlob = mock(Blob.class);
        BlobId quarantineObject = BlobId.of("quarantine", documentId);
        when(mockGoogleCloudStorageLibrary.get(quarantineObject)).thenReturn(mockBlob);

        // Execute
        ScanStatus status = storageProvider.getStatus(documentId);

        // Verify
        assertEquals(ScanStatus.FAILED_SCAN, status);
    }

    @Test
    void statusReady() {
        // Setup
        Blob mockBlob = mock(Blob.class);
        BlobId scannedObject = BlobId.of("scanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(scannedObject)).thenReturn(mockBlob);

        // Execute
        ScanStatus status = storageProvider.getStatus(documentId);

        // Verify
        assertEquals(ScanStatus.READY, status);
    }

    @Test
    void statusNotfound() {
        // Setup
        Blob mockBlob = mock(Blob.class);
        BlobId scannedObject = BlobId.of("scanned", "non-existent");
        when(mockGoogleCloudStorageLibrary.get(scannedObject)).thenReturn(mockBlob);
        // Verify
        assertThrows(ResponseStatusException.class, () -> storageProvider.getStatus(documentId));
    }

    @Test
    void metadata() {
        // Setup
        Blob mockBlob = mock(Blob.class);
        BlobId scannedObjectId = BlobId.of("scanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(scannedObjectId)).thenReturn(mockBlob);
        Map<String, String> gcpMetadata =
                Map.of("UPLOADED-BY", userId, "ORIGINAL-FILENAME", originalFilename);
        when(mockBlob.getMetadata()).thenReturn(gcpMetadata);

        // Execute
        DocumentMetadata response = storageProvider.getMetadata(documentId).get();

        assertEquals(userId, response.getUploadedBy());
        assertEquals(originalFilename, response.getOriginalFilename());
    }

    @Test
    void getMetadataWithNoBlob() {
        var e =
                assertThrows(
                        ResponseStatusException.class,
                        () -> storageProvider.getMetadata(documentId));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void getFileData() throws IOException {
        // Setup
        Blob mockBlob = mock(Blob.class);
        BlobId scannedObjectId = BlobId.of("scanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(scannedObjectId)).thenReturn(mockBlob);
        Map<String, String> gcpMetadata =
                Map.of("UPLOADED-BY", userId, "ORIGINAL-FILENAME", originalFilename);
        MediaType expectedMediaType = MediaType.TEXT_PLAIN;
        when(mockBlob.getMetadata()).thenReturn(gcpMetadata);
        when(mockBlob.getContent())
                .thenReturn(originalFileContents.getBytes(StandardCharsets.UTF_8));

        FileContent fileContent = storageProvider.getFileData(documentId);
        ByteArrayResource byteArrayResource = (ByteArrayResource) fileContent.getContent();

        assertEquals(expectedMediaType, fileContent.getContentType());
        assertArrayEquals(file.getBytes(), byteArrayResource.getByteArray());
    }

    @Test
    void getFileDataWithNonExistingBlobAccepted() {
        Blob mockBlob = mock(Blob.class);
        BlobId scannedObjectId = BlobId.of("scanned", documentId);
        BlobId unscannedObjectId = BlobId.of("unscanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(scannedObjectId)).thenReturn(null);
        when(mockGoogleCloudStorageLibrary.get(unscannedObjectId)).thenReturn(mock(Blob.class));
        Map<String, String> gcpMetadata =
                Map.of("UPLOADED-BY", userId, "ORIGINAL-FILENAME", originalFilename);
        when(mockBlob.getMetadata()).thenReturn(gcpMetadata);
        when(mockBlob.getContent())
                .thenReturn(originalFileContents.getBytes(StandardCharsets.UTF_8));

        var exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> storageProvider.getFileData(documentId));
        assertEquals(HttpStatus.ACCEPTED, exception.getStatusCode());
        assertEquals(
                "202 ACCEPTED \"Document awaiting scan not yet available. Try again later.\"",
                exception.getMessage());
    }

    @Test
    void getFileDataWithNonExistingBlobGone() {
        Blob mockBlob = mock(Blob.class);
        BlobId scannedObjectId = BlobId.of("scanned", documentId);
        BlobId quarantineObjectId = BlobId.of("quarantine", documentId);
        when(mockGoogleCloudStorageLibrary.get(scannedObjectId)).thenReturn(null);
        when(mockGoogleCloudStorageLibrary.get(quarantineObjectId)).thenReturn(mock(Blob.class));
        Map<String, String> gcpMetadata =
                Map.of("UPLOADED-BY", userId, "ORIGINAL-FILENAME", originalFilename);
        when(mockBlob.getMetadata()).thenReturn(gcpMetadata);
        when(mockBlob.getContent())
                .thenReturn(originalFileContents.getBytes(StandardCharsets.UTF_8));

        var exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> storageProvider.getFileData(documentId));
        assertEquals(HttpStatus.GONE, exception.getStatusCode());
        assertEquals(
                "410 GONE \"Document has been permanently quarantined and cannot be retrieved.\"",
                exception.getMessage());
    }

    @Test
    void getFileDataWithNonExistingBlobNotFound() {
        Blob mockBlob = mock(Blob.class);
        BlobId scannedObjectId = BlobId.of("scanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(scannedObjectId)).thenReturn(null);
        Map<String, String> gcpMetadata =
                Map.of("UPLOADED-BY", userId, "ORIGINAL-FILENAME", originalFilename);
        when(mockBlob.getMetadata()).thenReturn(gcpMetadata);
        when(mockBlob.getContent())
                .thenReturn(originalFileContents.getBytes(StandardCharsets.UTF_8));

        var exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> storageProvider.getFileData(documentId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("404 NOT_FOUND", exception.getMessage());
    }

    @Test
    void getUnscannedFileData() throws IOException {
        Blob mockBlob = mock(Blob.class);
        BlobId unscannedObjectId = BlobId.of("unscanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(unscannedObjectId)).thenReturn(mockBlob);
        Map<String, String> gcpMetadata =
                Map.of("UPLOADED-BY", userId, "ORIGINAL-FILENAME", originalFilename);
        MediaType expectedMediaType = MediaType.TEXT_PLAIN;
        when(mockBlob.getMetadata()).thenReturn(gcpMetadata);
        when(mockBlob.getContent())
                .thenReturn(originalFileContents.getBytes(StandardCharsets.UTF_8));

        FileContent fileContent = storageProvider.getUnscannedFileData(documentId);
        ByteArrayResource byteArrayResource = (ByteArrayResource) fileContent.getContent();

        assertEquals(expectedMediaType, fileContent.getContentType());
        assertArrayEquals(file.getBytes(), byteArrayResource.getByteArray());
    }

    @Test
    void getUnscannedFileDataWithNonExistingMetadata() {

        Blob mockBlob = mock(Blob.class);
        BlobId unscannedObjectId = BlobId.of("unscanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(unscannedObjectId)).thenReturn(mockBlob);

        var e =
                assertThrows(
                        ResponseStatusException.class,
                        () -> storageProvider.getUnscannedFileData(documentId));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void getFileDataWithNonExistingMetadata() {
        Blob mockBlob = mock(Blob.class);
        BlobId scannedObjectId = BlobId.of("scanned", documentId);
        when(mockGoogleCloudStorageLibrary.get(scannedObjectId)).thenReturn(mockBlob);
        var e =
                assertThrows(
                        ResponseStatusException.class,
                        () -> storageProvider.getFileData(documentId));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }
}
