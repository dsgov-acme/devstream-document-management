package io.nuvalence.ds4g.documentmanagement.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

/**
 * A utility to be used for files.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileUtils {

    /**
     * Gets the media type of the give file.
     * @param file The file to retrieve the media type of.
     * @return MediaType of the given file.
     * @throws IOException if media type cannot be retrieved.
     * @throws ResponseStatusException if file type is not found.
     */
    public static MediaType getMediaTypeOfFile(File file) throws IOException {
        String fileType = Files.probeContentType(file.toPath());
        if (fileType == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File type not found.");
        }
        return MediaType.valueOf(fileType);
    }

    /**
     * Gets contents of a file as a byte array resource.
     * @param file File to get Byte array resource from.
     * @return ByteArrayResource of the file contents.
     *
     * @throws ResponseStatusException if file is empty.
     * @throws UncheckedIOException if error occurs while processing file.
     */
    public static ByteArrayResource getByteArrayResourceFromFile(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] arr = new byte[(int) file.length()];
            int bytesRead = fileInputStream.read(arr);
            if (bytesRead == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File is empty.");
            }
            return new ByteArrayResource(arr);

        } catch (IOException e) {
            throw new UncheckedIOException("Error while processing the file.", e);
        }
    }
}
