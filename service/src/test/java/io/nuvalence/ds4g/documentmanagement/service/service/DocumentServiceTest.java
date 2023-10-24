package io.nuvalence.ds4g.documentmanagement.service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.nuvalence.ds4g.documentmanagement.service.entity.Document;
import io.nuvalence.ds4g.documentmanagement.service.repository.DocumentProcessorResultRepository;
import io.nuvalence.ds4g.documentmanagement.service.repository.DocumentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;

    @Mock private DocumentProcessorResultRepository documentProcessorResultRepository;

    private DocumentService service;

    @BeforeEach
    void setUp() {
        this.service = new DocumentService(documentRepository);
    }

    @Test
    void createDocumentTest_WithNoUploader() {
        Document document = createMockDocument();

        when(documentRepository.save(any(Document.class))).thenReturn(document);

        Document actual =
                service.createDocument(
                        document.getId().toString(),
                        document.getFilename(),
                        UUID.randomUUID().toString());

        assertEquals(document, actual);
    }

    @Test
    void createDocumentTest_WithUploader() {
        UUID mockUploaderId = UUID.randomUUID();

        Document document = createMockDocument();
        document.setUploadedBy(mockUploaderId.toString());

        when(documentRepository.save(any(Document.class))).thenReturn(document);

        Document savedDocument =
                service.createDocument(
                        document.getId().toString(),
                        document.getFilename(),
                        mockUploaderId.toString());

        assertEquals(mockUploaderId.toString(), savedDocument.getUploadedBy());
    }

    private Document createMockDocument() {
        return Document.builder().id(UUID.randomUUID()).filename("testFileName").build();
    }

    @Test
    void testGetUDocumentById_WhenDocumentExists() {
        // Arrange
        UUID documentId = UUID.randomUUID();
        Document expectedDocument = createMockDocument();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(expectedDocument));
        // Act
        Optional<Document> actualDocument = service.getUDocumentById(documentId);

        // Assert
        Assertions.assertTrue(actualDocument.isPresent());
        Assertions.assertEquals(expectedDocument, actualDocument.get());
    }

    @Test
    void testGetUDocumentById_WhenDocumentDoesNotExist() {
        // Arrange
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        // Act
        Optional<Document> actualDocument = service.getUDocumentById(documentId);

        // Assert
        Assertions.assertFalse(actualDocument.isPresent());
    }
}
