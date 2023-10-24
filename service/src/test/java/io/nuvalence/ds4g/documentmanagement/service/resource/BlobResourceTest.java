package io.nuvalence.ds4g.documentmanagement.service.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.InputStream;

class BlobResourceTest {

    private BlobResource blobResource1;
    private BlobResource blobResource2;

    private BlobResource blobResource3;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Blob mockBlob1 = createMockBlob("Blob1");
        Blob mockBlob2 = createMockBlob("Blob2");
        // Create BlobResource instances with mocked Blob objects
        blobResource1 = new BlobResource(mockBlob1);
        blobResource2 = new BlobResource(mockBlob2);
        blobResource3 = blobResource1;
    }

    @Test
    void testBlobResource() throws IOException {
        // Create a mock Blob instance
        Blob blob = mock(Blob.class);
        when(blob.toString()).thenReturn("MockBlob");

        try (ReadChannel byteChannel = Mockito.mock(ReadChannel.class)) {
            when(blob.reader()).thenReturn(byteChannel);

            // Create a BlobResource instance
            BlobResource blobResource = new BlobResource(blob);

            // Test getDescription() method
            String expectedDescription = "MockBlob";
            String actualDescription = blobResource.getDescription();
            Assertions.assertEquals(expectedDescription, actualDescription);

            // Test getInputStream() method
            try (InputStream inputStream = blobResource.getInputStream()) {
                Assertions.assertNotNull(inputStream);
            }
        }
    }

    @Test
    void testEquals_SameObject() {
        assertEquals(blobResource1, blobResource1);
    }

    @Test
    void testEquals_SameBlobs() {
        assertEquals(blobResource1, blobResource3);
    }

    @Test
    void testEquals_DifferentBlobs() {
        Blob mockBlob1 = createMockBlob("Blob1");
        Blob mockBlob2 = createMockBlob("Blob2");

        assertNotEquals(blobResource1, new BlobResource(mockBlob2));
        assertNotEquals(blobResource2, new BlobResource(mockBlob1));
    }

    @Test
    void testHashCode_DifferentBlobs() {
        Blob mockBlob1 = createMockBlob("Blob1");
        Blob mockBlob2 = createMockBlob("Blob2");

        assertNotEquals(blobResource1.hashCode(), new BlobResource(mockBlob2).hashCode());
        assertNotEquals(blobResource2.hashCode(), new BlobResource(mockBlob1).hashCode());
    }

    private Blob createMockBlob(String name) {
        Blob mockBlob = mock(Blob.class);
        when(mockBlob.toString()).thenReturn(name);
        return mockBlob;
    }
}
