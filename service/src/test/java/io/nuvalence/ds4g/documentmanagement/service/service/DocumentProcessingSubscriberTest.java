package io.nuvalence.ds4g.documentmanagement.service.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorResult;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorStatus;
import io.nuvalence.ds4g.documentmanagement.service.generated.models.DocumentProcessingRequest;
import io.nuvalence.ds4g.documentmanagement.service.processor.DocumentProcessingRequestWrapper;
import io.nuvalence.ds4g.documentmanagement.service.processor.exception.RetryableDocumentProcessingException;
import io.nuvalence.ds4g.documentmanagement.service.processor.exception.UnretryableDocumentProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class DocumentProcessingSubscriberTest {

    @Mock private ObjectMapper mapper;

    @Mock private DocumentProcessingService documentProcessingService;

    @Mock private DocumentProcessingSubscriber documentProcessingSubscriber;

    @BeforeEach
    void setUp() {
        this.documentProcessingSubscriber =
                new DocumentProcessingSubscriber(mapper, documentProcessingService);
    }

    private Message<byte[]> prepareMessage() throws JsonProcessingException {

        Message<byte[]> message = mock(Message.class);
        byte[] payload = "test payload".getBytes(StandardCharsets.UTF_8);
        when(message.getPayload()).thenReturn(payload);
        BasicAcknowledgeablePubsubMessage originalMessage =
                mock(BasicAcknowledgeablePubsubMessage.class);

        Map<String, Object> headers = new HashMap<>();
        headers.put(GcpPubSubHeaders.ORIGINAL_MESSAGE, originalMessage);
        MessageHeaders messageHeaders = new MessageHeaders(headers);
        when(message.getHeaders()).thenReturn(messageHeaders);

        DocumentProcessingRequestWrapper mockWrapper = mock(DocumentProcessingRequestWrapper.class);
        when(mockWrapper.getDocumentId()).thenReturn("id");
        when(mockWrapper.getRequest()).thenReturn(new DocumentProcessingRequest());
        when(mapper.readValue(any(String.class), eq(DocumentProcessingRequestWrapper.class)))
                .thenReturn(mockWrapper);

        return message;
    }

    @Test
    void testHandleMessage()
            throws JsonProcessingException, RetryableDocumentProcessingException,
                    UnretryableDocumentProcessingException {

        Message<byte[]> message = prepareMessage();

        when(documentProcessingService.processRequest(any(), any()))
                .thenReturn(
                        DocumentProcessorResult.builder()
                                .status(DocumentProcessorStatus.COMPLETE)
                                .build());

        documentProcessingSubscriber.handleMessage(message);

        verify(documentProcessingService)
                .processRequest(any(String.class), any(DocumentProcessingRequest.class));

        var originalMessage =
                message.getHeaders()
                        .get(
                                GcpPubSubHeaders.ORIGINAL_MESSAGE,
                                BasicAcknowledgeablePubsubMessage.class);

        verify(originalMessage).ack();
    }

    @Test
    void testHandleMessage_AllowRetries()
            throws JsonProcessingException, RetryableDocumentProcessingException,
                    UnretryableDocumentProcessingException {

        Message<byte[]> message = prepareMessage();

        when(documentProcessingService.processRequest(any(), any()))
                .thenReturn(
                        DocumentProcessorResult.builder()
                                .status(DocumentProcessorStatus.MISSING_DEPENDENCY)
                                .build());

        documentProcessingSubscriber.handleMessage(message);

        verify(documentProcessingService)
                .processRequest(any(String.class), any(DocumentProcessingRequest.class));

        var originalMessage =
                message.getHeaders()
                        .get(
                                GcpPubSubHeaders.ORIGINAL_MESSAGE,
                                BasicAcknowledgeablePubsubMessage.class);

        verify(originalMessage, never()).ack();
    }

    @Test
    void handleMessage_shouldCatchJsonProcessingException(CapturedOutput output)
            throws MessagingException, JsonProcessingException,
                    RetryableDocumentProcessingException, UnretryableDocumentProcessingException {

        Message<byte[]> message = prepareMessage();

        doThrow(JsonProcessingException.class)
                .when(documentProcessingService)
                .processRequest(any(), any());

        documentProcessingSubscriber.handleMessage(message);

        assertTrue(output.getOut().contains("An error occurred processing request"));
    }
}
