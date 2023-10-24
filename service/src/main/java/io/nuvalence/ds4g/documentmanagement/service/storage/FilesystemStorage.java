package io.nuvalence.ds4g.documentmanagement.service.storage;

import io.nuvalence.ds4g.documentmanagement.service.model.DocumentMetadata;
import io.nuvalence.ds4g.documentmanagement.service.model.FileContent;
import io.nuvalence.ds4g.documentmanagement.service.model.FilesystemDocumentMetadata;
import io.nuvalence.ds4g.documentmanagement.service.model.ScanStatus;
import io.nuvalence.ds4g.documentmanagement.service.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Stores uploaded documents to local filesystem.
 *
 * <p>
 * This class is provided to enable local development & testing without having
 * access to cloud
 * storage resources. Uploaded documents are stored in a new temporary directory
 * created each
 * time the service is started.
 * </p>
 *
 * <p>
 * This implementation does not perform virus scanning like the production
 * environment. However, it
 * does simulate the process of moving documents between unscanned, quarantine,
 * and clean buckets
 * in order to test behavior of the STATUS and DOWNLOAD endpoints. Behavior is
 * controlled by the
 * filename used during upload.
 * </p>
 *
 * <ul>
 * <li>Filename contains the word QUARANTINE: Treat file as if it has failed
 * virus scanning and is moved to
 * quarantine bucket.</li>
 * <li>Filename contains the word WAIT: Treat file as if it is always awaiting
 * scanning.</li>
 * <li>All other files: Treat as having passed virus scanning & be immediately
 * available for download.</li>
 * </ul>
 */
@ConditionalOnProperty(name = "document-management.storage-provider", havingValue = "filesystem")
@Service
@Slf4j
@SuppressWarnings("ClassDataAbstractionCoupling")
public class FilesystemStorage implements StorageProvider {

    private final File unscannedBucket;
    private final File quarantineBucket;
    private final File scannedBucket;
    private final ConcurrentMap<String, FilesystemDocumentMetadata> fsMetadata =
            new ConcurrentHashMap<>();

    /**
     * Constructor.
     * Creates temporary directories for storing uploaded files.
     * 
     * @throws IOException if temporary directories cannot be created.
     */
    public FilesystemStorage() throws IOException {
        String exportTempDir = "./data/files";
        Path parentDir = Paths.get(exportTempDir);
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        File rootFile;
        if (SystemUtils.IS_OS_UNIX) {
            FileAttribute<Set<PosixFilePermission>> attr =
                    PosixFilePermissions.asFileAttribute(
                            PosixFilePermissions.fromString("rwx------"));
            rootFile = Files.createTempDirectory(parentDir, "upload", attr).toFile();
        } else {
            rootFile = Files.createTempDirectory(parentDir, "upload").toFile();
            setFilePermissions(rootFile);
        }

        unscannedBucket = new File(rootFile, "unscanned");
        createDirectory(unscannedBucket);

        quarantineBucket = new File(rootFile, "quarantine");
        createDirectory(quarantineBucket);

        scannedBucket = new File(rootFile, "scanned");
        createDirectory(scannedBucket);
    }

    private static void setFilePermissions(File file) throws IOException {
        if (!file.setReadable(true, true) || !file.setWritable(true, true)) {
            throw new IOException(
                    "Failed to set permissions for the file: " + file.getAbsolutePath());
        }
    }

    private static void createDirectory(File file) throws IOException {
        if (!file.mkdirs()) {
            throw new IOException("Failed to create directory: " + file.getAbsolutePath());
        }
    }

    @Override
    public String upload(MultipartFile file, DocumentMetadata documentMetadata)
            throws ResponseStatusException {
        UUID uuid = UUID.randomUUID();
        String documentId = uuid.toString();

        fsMetadata.put(documentId, new FilesystemDocumentMetadata(file, documentMetadata));
        File destination = scannedBucket;

        // Simulate progress through ingestion based on original filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            if (originalFilename.contains("WAIT")) {
                destination = unscannedBucket;
            } else if (originalFilename.contains("QUARANTINE")) {
                destination = quarantineBucket;
            }
        }

        Path filepath = Path.of(destination.getAbsolutePath(), documentId);
        try (FileOutputStream fileStream = new FileOutputStream(filepath.toString())) {
            fileStream.write(file.getBytes());
            log.info("Uploaded file saved to {}", filepath);
            return documentId;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded file locally", e);
        }
    }

    @Override
    public ScanStatus getStatus(String documentId) throws ResponseStatusException {
        if (fileExists(scannedBucket, documentId)) {
            return ScanStatus.READY;
        } else if (fileExists(unscannedBucket, documentId)) {
            return ScanStatus.AWAITING_SCAN;
        } else if (fileExists(quarantineBucket, documentId)) {
            return ScanStatus.FAILED_SCAN;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public Optional<DocumentMetadata> getMetadata(String documentId)
            throws ResponseStatusException {
        FilesystemDocumentMetadata metadata = fsMetadata.get(documentId);
        if (metadata == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found.");
        }

        return Optional.ofNullable(metadata.getDocumentMetadata());
    }

    @Override
    public Optional<DocumentMetadata> getUnscannedMetadata(String documentId)
            throws ResponseStatusException {

        throw new UnsupportedOperationException("Unimplemented method 'getUnscannedMetadata'");
    }

    @Override
    public FileContent getFileData(String documentId) throws ResponseStatusException, IOException {
        File f = new File(scannedBucket, documentId);
        return getDataFromFile(f, documentId);
    }

    @Override
    public FileContent getUnscannedFileData(String documentId)
            throws ResponseStatusException, IOException {
        File f = new File(unscannedBucket, documentId);
        return getDataFromFile(f, documentId);
    }

    private FileContent getDataFromFile(File f, String documentId) throws IOException {
        if (f.exists()) {
            Optional<DocumentMetadata> documentMetadata = this.getMetadata(documentId);
            if (documentMetadata.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document metadata not found.");
            }
            File originalFile = new File(documentMetadata.get().getOriginalFilename());

            MediaType mediaType = FileUtils.getMediaTypeOfFile(originalFile);
            return new FileContent(FileUtils.getByteArrayResourceFromFile(f), mediaType);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found.");
        }
    }

    private boolean fileExists(File parentDirectory, String filename) {
        return new File(parentDirectory, filename).exists();
    }

    @Override
    public void quaranfineFile(String documentId) {

        throw new UnsupportedOperationException("Unimplemented method 'quaranfineFile'");
    }

    @Override
    public void confirmCleanFile(String documentId) {
        throw new UnsupportedOperationException("Unimplemented method 'confirmCleanFile'");
    }
}
