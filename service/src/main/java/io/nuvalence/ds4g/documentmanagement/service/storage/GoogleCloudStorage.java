package io.nuvalence.ds4g.documentmanagement.service.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import io.nuvalence.ds4g.documentmanagement.service.model.DocumentMetadata;
import io.nuvalence.ds4g.documentmanagement.service.model.FileContent;
import io.nuvalence.ds4g.documentmanagement.service.model.ScanStatus;
import io.nuvalence.ds4g.documentmanagement.service.util.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores uploaded documents in Google Cloud Storage.
 * Documents are stored in an "unscanned" bucket to await antivirus scanning.
 */
@ConditionalOnProperty(name = "document-management.storage-provider", havingValue = "google")
@Service
public class GoogleCloudStorage implements StorageProvider {

    private static final String DOCUMENT_METADATA_NOT_FOUND = "Document metadata not found";
    private final String unscannedBucket;
    private final String quarantineBucket;
    private final String scannedBucket;
    private final Storage storage;

    /**
     * Constructor for Google cloud storage provider with autowired parameters.
     *
     * @param unscannedBucket          Bucket name where documents will initally be
     *                                 uploaded to, to await scanning.
     * @param quarantineBucket         Bucket name for documents that fail malware
     *                                 scanning.
     * @param scannedBucket            Bucket name for documents that pass malware
     *                                 scanning, and can be retrieved.
     * @param storage                  Google Cloud Storage library.
     */
    public GoogleCloudStorage(
            @Value("${document-management.google.bucket.unscanned-files}") String unscannedBucket,
            @Value("${document-management.google.bucket.quarantined-files}")
                    String quarantineBucket,
            @Value("${document-management.google.bucket.scanned-files}") String scannedBucket,
            Storage storage) {
        this.unscannedBucket = unscannedBucket;
        this.quarantineBucket = quarantineBucket;
        this.scannedBucket = scannedBucket;
        this.storage = storage;
    }

    @Override
    public String upload(MultipartFile file, DocumentMetadata metadata)
            throws ResponseStatusException {
        UUID uuid = UUID.randomUUID();
        String documentId = uuid.toString();

        BlobId blobId = BlobId.of(unscannedBucket, documentId);
        BlobInfo.Builder blobInfo =
                BlobInfo.newBuilder(blobId)
                        .setContentType(file.getContentType())
                        .setMetadata(metadata.toCloudStorageMetadataFormat());

        try {
            storage.create(blobInfo.build(), file.getBytes());
            return documentId;
        } catch (StorageException | IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to google cloud.", e);
        }
    }

    @Override
    public ScanStatus getStatus(String documentId) throws ResponseStatusException {
        if (documentInBucket(scannedBucket, documentId)) {
            return ScanStatus.READY;
        }

        if (documentInBucket(quarantineBucket, documentId)) {
            return ScanStatus.FAILED_SCAN;
        }

        if (documentInBucket(unscannedBucket, documentId)) {
            return ScanStatus.AWAITING_SCAN;
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private boolean documentInBucket(String bucketName, String documentId) {
        BlobId blobId = BlobId.of(bucketName, documentId);
        Blob blob = storage.get(blobId);
        return blob != null;
    }

    @Override
    public Optional<DocumentMetadata> getMetadata(String documentId)
            throws ResponseStatusException {
        Blob blob = storage.get(BlobId.of(scannedBucket, documentId));
        return getMetadataFromBlob(blob, documentId);
    }

    @Override
    public Optional<DocumentMetadata> getUnscannedMetadata(String documentId)
            throws ResponseStatusException {
        Blob blob = storage.get(BlobId.of(unscannedBucket, documentId));
        return getMetadataFromBlob(blob, documentId);
    }

    private Optional<DocumentMetadata> getMetadataFromBlob(Blob blob, String documentId)
            throws ResponseStatusException {
        if (blob == null) {
            ScanStatus status = getStatus(documentId);
            throw new ResponseStatusException(status.getStatus(), status.getMessage());
        }

        return Optional.of(new DocumentMetadata(blob.getMetadata()));
    }

    @Override
    public FileContent getFileData(String documentId) throws ResponseStatusException, IOException {
        Blob blob = storage.get(BlobId.of(scannedBucket, documentId));

        Optional<DocumentMetadata> documentMetadata = this.getMetadata(documentId);

        if (documentMetadata.isPresent()
                && (documentMetadata.get().getOriginalFilename() == null)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, DOCUMENT_METADATA_NOT_FOUND);
        }

        return getDataFromBlob(blob, documentMetadata);
    }

    @Override
    public FileContent getUnscannedFileData(String documentId)
            throws ResponseStatusException, IOException {
        Blob blob = storage.get(BlobId.of(unscannedBucket, documentId));
        Optional<DocumentMetadata> documentMetadata = this.getUnscannedMetadata(documentId);

        if (documentMetadata.isPresent()
                && (documentMetadata.get().getOriginalFilename() == null)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, DOCUMENT_METADATA_NOT_FOUND);
        }

        return getDataFromBlob(blob, documentMetadata);
    }

    private FileContent getDataFromBlob(Blob blob, Optional<DocumentMetadata> documentMetadata)
            throws ResponseStatusException, IOException {

        if (documentMetadata.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, DOCUMENT_METADATA_NOT_FOUND);
        }

        File originalFile = new File(documentMetadata.get().getOriginalFilename());

        MediaType mediaType = FileUtils.getMediaTypeOfFile(originalFile);

        ByteArrayResource resource = new ByteArrayResource(blob.getContent());

        return new FileContent(resource, mediaType);
    }

    @Override
    public void quaranfineFile(String documentId) throws IOException {
        moveFile(documentId, unscannedBucket, quarantineBucket);
    }

    @Override
    public void confirmCleanFile(String documentId) throws IOException {
        moveFile(documentId, unscannedBucket, scannedBucket);
    }

    private void moveFile(String documentId, String srcBucket, String targetBucket)
            throws IOException {
        BlobId srcBlobId = BlobId.of(srcBucket, documentId);

        // Get source blob
        Blob srcBlob = storage.get(srcBlobId);

        // Copy source blob to target bucket
        Blob targetBlob = srcBlob.copyTo(targetBucket, documentId).getResult();
        if (!targetBlob.exists()) {
            throw new IOException(
                    "Failed to copy " + documentId + " to " + targetBucket + " bucket");
        }

        // Delete source blob
        if (!srcBlob.delete()) {
            throw new IOException(
                    "Failed to delete " + documentId + " from " + srcBucket + " bucket");
        }
    }
}
