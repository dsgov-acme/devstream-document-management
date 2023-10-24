package io.nuvalence.ds4g.documentmanagement.service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.ds4g.documentmanagement.service.config.PublishDocumentProcessingConfig;
import io.nuvalence.ds4g.documentmanagement.service.config.PublishDocumentProcessingResultConfig;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorResult;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorStatus;
import io.nuvalence.ds4g.documentmanagement.service.generated.models.DocumentProcessingRequest;
import io.nuvalence.ds4g.documentmanagement.service.processor.DocumentProcessingRequestWrapper;
import io.nuvalence.ds4g.documentmanagement.service.processor.DocumentProcessor;
import io.nuvalence.ds4g.documentmanagement.service.processor.DocumentProcessorRegistry;
import io.nuvalence.ds4g.documentmanagement.service.processor.exception.RetryableDocumentProcessingException;
import io.nuvalence.ds4g.documentmanagement.service.processor.exception.UnretryableDocumentProcessingException;
import io.nuvalence.ds4g.documentmanagement.service.repository.DocumentProcessorResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class DocumentProcessingServiceTest {

    private DocumentProcessingService documentProcessingService;

    @Mock private DocumentProcessorRegistry documentProcessorRegistry;

    @Mock private DocumentProcessor documentProcessor;

    @Mock private ObjectMapper mapper;

    @Mock
    private PublishDocumentProcessingConfig.DocumentProcessingPublisher documentProcessingPublisher;

    @Mock private DocumentProcessorResultRepository documentProcessorResultRepository;

    @Mock
    private PublishDocumentProcessingResultConfig.DocumentProcessingResultPublisher
            documentProcessingResultPublisher;

    @Captor private ArgumentCaptor<DocumentProcessorResult> captor;

    @BeforeEach
    void setUp() {
        this.documentProcessingService =
                new DocumentProcessingService(
                        documentProcessingPublisher,
                        documentProcessingResultPublisher,
                        documentProcessorRegistry,
                        mapper,
                        documentProcessorResultRepository);
    }

    @Test
    void testEnqueueDocumentProcessingRequest() throws JsonProcessingException {
        String id = UUID.randomUUID().toString();
        DocumentProcessingRequest request1 = new DocumentProcessingRequest();
        request1.setProcessorId("processorOne");
        DocumentProcessingRequest request2 = new DocumentProcessingRequest();
        request2.setProcessorId("processorTwo");
        List<DocumentProcessingRequest> requests = Arrays.asList(request1, request2);

        DocumentProcessingRequestWrapper wrapper1 =
                DocumentProcessingRequestWrapper.builder().request(request1).documentId(id).build();

        DocumentProcessingRequestWrapper wrapper2 =
                DocumentProcessingRequestWrapper.builder().request(request2).documentId(id).build();

        when(mapper.writeValueAsString(wrapper1)).thenReturn("json1");
        when(mapper.writeValueAsString(wrapper2)).thenReturn("json2");

        documentProcessingService.enqueueDocumentProcessingRequest(id, requests);

        verify(documentProcessorResultRepository, times(2)).save(captor.capture());
        List<DocumentProcessorResult> enqueuedList = captor.getAllValues();

        assertEquals(2, enqueuedList.size());
        assertEquals(id, enqueuedList.get(0).getDocumentId().toString());
        assertEquals(id, enqueuedList.get(1).getDocumentId().toString());
        assertEquals(request1.getProcessorId(), enqueuedList.get(0).getProcessorId());
        assertEquals(request2.getProcessorId(), enqueuedList.get(1).getProcessorId());
        assertEquals(DocumentProcessorStatus.PENDING, enqueuedList.get(0).getStatus());
        assertEquals(DocumentProcessorStatus.PENDING, enqueuedList.get(1).getStatus());

        verify(mapper, times(1)).writeValueAsString(wrapper1);
        verify(mapper, times(1)).writeValueAsString(wrapper2);
        verify(documentProcessingPublisher, times(1)).publish("json1");
        verify(documentProcessingPublisher, times(1)).publish("json2");
    }

    @Test
    void testProcessRequestSuccess()
            throws JsonProcessingException, RetryableDocumentProcessingException,
                    UnretryableDocumentProcessingException {
        String processorId = "testProcessorId";
        String documentId = UUID.randomUUID().toString();
        DocumentProcessorResult result = new DocumentProcessorResult();
        result.setStatus(DocumentProcessorStatus.COMPLETE);
        String testString = "testString";

        when(documentProcessorRegistry.getProcessor(processorId))
                .thenReturn(Optional.of(documentProcessor));
        when(documentProcessor.process(documentId)).thenReturn(result);
        when(mapper.writeValueAsString(any())).thenReturn(testString);
        doNothing().when(documentProcessingResultPublisher).publish(testString);

        DocumentProcessingRequest request = new DocumentProcessingRequest();
        request.setProcessorId(processorId);

        documentProcessingService.processRequest(documentId, request);

        verify(documentProcessorRegistry, times(1)).getProcessor(processorId);
        verify(documentProcessor, times(1)).process(documentId);
        verify(documentProcessorResultRepository, times(1)).save(result);
        verify(documentProcessingResultPublisher, times(1)).publish(testString);
    }

    @Test
    void testProcessRequestWithNonExistingProcessor() {
        String processorId = "testProcessorId";
        String documentId = UUID.randomUUID().toString();

        DocumentProcessingRequest request = new DocumentProcessingRequest();
        request.setProcessorId(processorId);

        UnretryableDocumentProcessingException exception =
                assertThrows(
                        UnretryableDocumentProcessingException.class,
                        () -> {
                            documentProcessingService.processRequest(documentId, request);
                        });

        verify(documentProcessorResultRepository, times(1)).save(any());

        String expectedMessage = "Processor not found";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testProcessRequestWithMissingDependencies() {
        String processorId = "testProcessorId";
        String documentId = UUID.randomUUID().toString();
        DocumentProcessorResult result = new DocumentProcessorResult();
        result.setStatus(DocumentProcessorStatus.MISSING_DEPENDENCY);

        when(documentProcessorRegistry.getProcessor(processorId))
                .thenReturn(Optional.of(documentProcessor));
        when(documentProcessor.process(documentId)).thenReturn(result);

        DocumentProcessingRequest request = new DocumentProcessingRequest();
        request.setProcessorId(processorId);

        RetryableDocumentProcessingException exception =
                assertThrows(
                        RetryableDocumentProcessingException.class,
                        () -> {
                            documentProcessingService.processRequest(documentId, request);
                        });

        String expectedMessage = "Missing dependency";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testProcessRequestWithUnprocessableWithErrorMessage() {
        String processorId = "testProcessorId";
        DocumentProcessorResult result = new DocumentProcessorResult();
        result.setStatus(DocumentProcessorStatus.UNPROCESSABLE);
        result.setResult(Map.of("error", "testError"));
        String documentId = UUID.randomUUID().toString();

        when(documentProcessorRegistry.getProcessor(processorId))
                .thenReturn(Optional.of(documentProcessor));
        when(documentProcessor.process(documentId)).thenReturn(result);

        DocumentProcessingRequest request = new DocumentProcessingRequest();
        request.setProcessorId(processorId);

        UnretryableDocumentProcessingException exception =
                assertThrows(
                        UnretryableDocumentProcessingException.class,
                        () -> {
                            documentProcessingService.processRequest(documentId, request);
                        });

        String expectedMessage = "testError";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testProcessRequestWithUnprocessableNoErrorMessage() {
        String processorId = "testProcessorId";
        String documentId = UUID.randomUUID().toString();
        DocumentProcessorResult result = new DocumentProcessorResult();
        result.setStatus(DocumentProcessorStatus.UNPROCESSABLE);

        when(documentProcessorRegistry.getProcessor(processorId))
                .thenReturn(Optional.of(documentProcessor));
        when(documentProcessor.process(documentId)).thenReturn(result);

        DocumentProcessingRequest request = new DocumentProcessingRequest();
        request.setProcessorId(processorId);

        UnretryableDocumentProcessingException exception =
                assertThrows(
                        UnretryableDocumentProcessingException.class,
                        () -> {
                            documentProcessingService.processRequest(documentId, request);
                        });

        String expectedMessage = "An unretryable error occurred";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testProcessRequestWithNotComplete() {
        String processorId = "testProcessorId";
        String documentId = UUID.randomUUID().toString();
        DocumentProcessorResult result = new DocumentProcessorResult();
        result.setStatus(DocumentProcessorStatus.RETRYABLE_ERROR);

        when(documentProcessorRegistry.getProcessor(processorId))
                .thenReturn(Optional.of(documentProcessor));
        when(documentProcessor.process(documentId)).thenReturn(result);

        DocumentProcessingRequest request = new DocumentProcessingRequest();
        request.setProcessorId(processorId);

        RetryableDocumentProcessingException exception =
                assertThrows(
                        RetryableDocumentProcessingException.class,
                        () -> {
                            documentProcessingService.processRequest(documentId, request);
                        });

        String expectedMessage =
                String.format(
                        "Document %s request could not be completed and will be retried. Status:"
                                + " RETRYABLE_ERROR",
                        documentId);
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testGetProcessingResultsForDocument() {
        UUID documentId = UUID.randomUUID();
        DocumentProcessorResult result1 =
                DocumentProcessorResult.builder()
                        .documentId(documentId)
                        .timestamp(OffsetDateTime.now().minusDays(1))
                        .build();
        DocumentProcessorResult result2 =
                DocumentProcessorResult.builder()
                        .documentId(documentId)
                        .timestamp(OffsetDateTime.now().minusDays(2))
                        .build();
        DocumentProcessorResult result3 =
                DocumentProcessorResult.builder()
                        .documentId(documentId)
                        .timestamp(OffsetDateTime.now())
                        .build();

        List<DocumentProcessorResult> unsortedList = Arrays.asList(result1, result2, result3);
        List<DocumentProcessorResult> expectedList = Arrays.asList(result2, result1, result3);

        when(documentProcessorResultRepository.findByDocumentId(documentId))
                .thenReturn(unsortedList);

        List<DocumentProcessorResult> actualList =
                documentProcessingService.getProcessingResultsForDocument(documentId);

        assertArrayEquals(expectedList.toArray(), actualList.toArray());
        verify(documentProcessorResultRepository, times(1)).findByDocumentId(documentId);
    }
}
