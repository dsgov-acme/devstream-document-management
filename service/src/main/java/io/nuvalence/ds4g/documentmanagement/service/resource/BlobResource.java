package io.nuvalence.ds4g.documentmanagement.service.resource;

import com.google.cloud.storage.Blob;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.AbstractResource;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.Objects;

/**
 * Spring Resource wrapper for GCP Cloud Storage Blob objects.
 */
@RequiredArgsConstructor
public class BlobResource extends AbstractResource {
    private final Blob blob;

    @Override
    @NotNull public String getDescription() {
        return blob.toString();
    }

    @Override
    @NotNull public InputStream getInputStream() {
        return Channels.newInputStream(blob.reader());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BlobResource that = (BlobResource) o;
        return Objects.equals(blob, that.blob);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blob);
    }
}
