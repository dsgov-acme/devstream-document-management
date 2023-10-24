package io.nuvalence.ds4g.documentmanagement.service.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.documentai.v1beta3.Document;
import com.google.cloud.documentai.v1beta3.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1beta3.ProcessRequest;
import com.google.cloud.documentai.v1beta3.ProcessResponse;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorStatus;
import io.nuvalence.ds4g.documentmanagement.service.model.FileContent;
import io.nuvalence.ds4g.documentmanagement.service.model.ScanStatus;
import io.nuvalence.ds4g.documentmanagement.service.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class DocumentQualityProcessorTest {

    private static StorageProvider storage;
    private static DocumentQualityProcessor processor;
    private final Random rd = new Random();
    @Mock private DocumentProcessorServiceClient documentProcessorClient;

    @BeforeEach
    public void setUp() throws IOException {
        storage = Mockito.mock(StorageProvider.class);
        processor = new DocumentQualityProcessor("processorId");
        processor.setStorage(storage);
    }

    @Test
    void process() throws IOException {

        ReflectionTestUtils.setField(processor, "client", documentProcessorClient);
        ReflectionTestUtils.setField(processor, "projectId", "dsgov-dev");
        ReflectionTestUtils.setField(processor, "location", "us");

        FileContent fileData = Mockito.mock(FileContent.class);
        byte[] bytes = new byte[7];
        rd.nextBytes(bytes);

        Resource resource = Mockito.mock(Resource.class);

        Document.Page page =
                Document.Page.newBuilder()
                        .setImageQualityScores(
                                Document.Page.ImageQualityScores.newBuilder()
                                        .setQualityScore(0.7811847f)
                                        .addDetectedDefects(
                                                Document.Page.ImageQualityScores.DetectedDefect
                                                        .newBuilder()
                                                        .setType("quality/defect_document_cutoff")
                                                        .setConfidence(1)
                                                        .build())
                                        .addDetectedDefects(
                                                Document.Page.ImageQualityScores.DetectedDefect
                                                        .newBuilder()
                                                        .setType("quality/defect_glare")
                                                        .setConfidence(0.97849524f)
                                                        .build())
                                        .addDetectedDefects(
                                                Document.Page.ImageQualityScores.DetectedDefect
                                                        .newBuilder()
                                                        .setType("quality/defect_text_cutoff")
                                                        .setConfidence(0.5f)
                                                        .build())
                                        .build())
                        .build();
        Document document = Document.newBuilder().addPages(page).build();
        ProcessResponse processorResult =
                ProcessResponse.newBuilder().setDocument(document).build();

        UUID documentId = UUID.randomUUID();
        Mockito.when(fileData.getContent()).thenReturn(resource);
        Mockito.when(fileData.getContentType()).thenReturn(MediaType.APPLICATION_PDF);
        Mockito.when(resource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(bytes));
        Mockito.when(storage.getFileData(documentId.toString())).thenReturn(fileData);
        Mockito.when(documentProcessorClient.processDocument(Mockito.any(ProcessRequest.class)))
                .thenReturn(processorResult);

        var result = processor.process(documentId.toString());

        assertEquals(DocumentProcessorStatus.COMPLETE, result.getStatus());
        assertEquals(documentId, result.getDocumentId());
    }

    @Test
    void testParseResults() {
        Document.Page page =
                Document.Page.newBuilder()
                        .setImageQualityScores(
                                Document.Page.ImageQualityScores.newBuilder()
                                        .setQualityScore(0.7811847f)
                                        .addDetectedDefects(
                                                Document.Page.ImageQualityScores.DetectedDefect
                                                        .newBuilder()
                                                        .setType("quality/defect_document_cutoff")
                                                        .setConfidence(1)
                                                        .build())
                                        .addDetectedDefects(
                                                Document.Page.ImageQualityScores.DetectedDefect
                                                        .newBuilder()
                                                        .setType("quality/defect_glare")
                                                        .setConfidence(0.97849524f)
                                                        .build())
                                        .addDetectedDefects(
                                                Document.Page.ImageQualityScores.DetectedDefect
                                                        .newBuilder()
                                                        .setType("quality/defect_text_cutoff")
                                                        .setConfidence(0.5f)
                                                        .build())
                                        .build())
                        .build();
        Document document = Document.newBuilder().addPages(page).build();
        ProcessResponse response = ProcessResponse.newBuilder().setDocument(document).build();

        DocumentQualityProcessorResult result = processor.parseResults(response);
        assertEquals(document.getPagesCount(), result.getPages().size());
        assertEquals(page.getImageQualityScores().getQualityScore(), result.getQualityScore());
    }

    @Test
    void processException() throws IOException {

        UUID docId = UUID.randomUUID();
        Mockito.when(storage.getFileData(docId.toString()))
                .thenThrow(new RuntimeException("exception message"));

        var result = processor.process(docId.toString());

        assertEquals(DocumentProcessorStatus.UNPROCESSABLE, result.getStatus());
        assert result.getResult() instanceof Map;
        var resultObj = (Map<?, ?>) result.getResult();
        assertEquals("exception message", resultObj.get("error"));
    }

    @Test
    void processAwaitingScan() {

        UUID docId = UUID.randomUUID();
        Mockito.when(storage.getStatus(docId.toString())).thenReturn(ScanStatus.AWAITING_SCAN);

        var result = processor.process(docId.toString());

        assertEquals(DocumentProcessorStatus.MISSING_DEPENDENCY, result.getStatus());
        assert result.getResult() instanceof Map;
        var resultObj = (Map<?, ?>) result.getResult();
        assertEquals("Waiting for processor dependency", resultObj.get("state"));
    }

    @Test
    void processFailedScan() throws IOException {

        UUID docId = UUID.randomUUID();
        ScanStatus failedScanStatus = ScanStatus.FAILED_SCAN;
        Mockito.when(storage.getStatus(docId.toString())).thenReturn(failedScanStatus);
        Mockito.when(storage.getFileData(docId.toString()))
                .thenThrow(
                        new ResponseStatusException(
                                failedScanStatus.getStatus(), failedScanStatus.getMessage()));

        var result = processor.process(docId.toString());

        assertEquals(DocumentProcessorStatus.UNPROCESSABLE, result.getStatus());
        assert result.getResult() instanceof Map;
        var resultObj = (Map<?, ?>) result.getResult();
        assertEquals(
                failedScanStatus.getStatus() + " \"" + failedScanStatus.getMessage() + "\"",
                resultObj.get("error"));
    }

    @Test
    void processApiExceptionRetryable() throws IOException {
        ReflectionTestUtils.setField(processor, "client", documentProcessorClient);
        ReflectionTestUtils.setField(processor, "projectId", "dsgov-dev");
        ReflectionTestUtils.setField(processor, "location", "us");

        FileContent fileData = Mockito.mock(FileContent.class);
        Resource resource = Mockito.mock(Resource.class);
        byte[] bytes = new byte[7];
        rd.nextBytes(bytes);
        Mockito.when(fileData.getContent()).thenReturn(resource);
        Mockito.when(fileData.getContentType()).thenReturn(MediaType.APPLICATION_PDF);
        Mockito.when(resource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(bytes));
        UUID documentId = UUID.randomUUID();
        Mockito.when(storage.getFileData(documentId.toString())).thenReturn(fileData);

        ApiException apiException =
                new ApiException(
                        new Throwable("Cause description"),
                        new StatusCode() {
                            @Override
                            public Code getCode() {
                                return Code.DEADLINE_EXCEEDED;
                            }

                            @Override
                            public Object getTransportCode() {
                                return null;
                            }
                        },
                        true);
        Mockito.when(documentProcessorClient.processDocument(Mockito.any(ProcessRequest.class)))
                .thenThrow(apiException);

        var result = processor.process(documentId.toString());

        assertEquals(DocumentProcessorStatus.RETRYABLE_ERROR, result.getStatus());
    }

    @Test
    void processApiExceptionUnretryable() throws IOException {
        ReflectionTestUtils.setField(processor, "client", documentProcessorClient);
        ReflectionTestUtils.setField(processor, "projectId", "dsgov-dev");
        ReflectionTestUtils.setField(processor, "location", "us");

        FileContent fileData = Mockito.mock(FileContent.class);
        Resource resource = Mockito.mock(Resource.class);
        byte[] bytes = new byte[7];
        rd.nextBytes(bytes);
        Mockito.when(fileData.getContent()).thenReturn(resource);
        Mockito.when(fileData.getContentType()).thenReturn(MediaType.APPLICATION_PDF);
        Mockito.when(resource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(bytes));
        UUID documentId = UUID.randomUUID();
        Mockito.when(storage.getFileData(documentId.toString())).thenReturn(fileData);

        ApiException apiException =
                new ApiException(
                        new Throwable("Cause description"),
                        new StatusCode() {
                            @Override
                            public Code getCode() {
                                return Code.INVALID_ARGUMENT;
                            }

                            @Override
                            public Object getTransportCode() {
                                return null;
                            }
                        },
                        false);
        Mockito.when(documentProcessorClient.processDocument(Mockito.any(ProcessRequest.class)))
                .thenThrow(apiException);

        var result = processor.process(documentId.toString());

        assertEquals(DocumentProcessorStatus.UNPROCESSABLE, result.getStatus());
    }
}
