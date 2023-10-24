package io.nuvalence.ds4g.documentmanagement.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorStatus;
import io.nuvalence.ds4g.documentmanagement.service.processor.DocumentProcessingRequestWrapper;
import io.nuvalence.ds4g.documentmanagement.service.processor.exception.RetryableDocumentProcessingException;
import io.nuvalence.ds4g.documentmanagement.service.processor.exception.UnretryableDocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Service to handle messages from the PubSub subscription, for document processing.
 */
@ConditionalOnProperty(name = "document-management.pubsub.enabled", havingValue = "true")
@Service
@Slf4j
public class DocumentProcessingSubscriber implements MessageHandler {

    private static final String ERROR_PARSING_MESSAGE = "Error parsing message from PubSub";
    private final ObjectMapper mapper;

    private final DocumentProcessingService documentProcessingService;

    /**
     * Subscriber constructor.
     *
     * @param mapper                    the object mapper bean
     * @param documentProcessingService service to process documents.
     */
    public DocumentProcessingSubscriber(
            ObjectMapper mapper, DocumentProcessingService documentProcessingService) {
        this.mapper = mapper;
        this.documentProcessingService = documentProcessingService;
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {

        log.trace("Received message for document processing.");

        DocumentProcessingRequestWrapper requestWrapper = unsafeParseRequest(message);

        try {
            var result =
                    documentProcessingService.processRequest(
                            requestWrapper.getDocumentId(), requestWrapper.getRequest());

            if (result != null && result.getStatus() != null) {
                var status = result.getStatus();
                if (status != DocumentProcessorStatus.MISSING_DEPENDENCY
                        && status != DocumentProcessorStatus.PENDING) {
                    acknowledgeMessage(message, true);
                }
            }

        } catch (JsonProcessingException e) {
            log.error("An error occurred processing request", e);
            acknowledgeMessage(message, true);
        } catch (UnretryableDocumentProcessingException e) {
            acknowledgeMessage(message, true);
        } catch (RetryableDocumentProcessingException e) {
            acknowledgeMessage(message, false);
        }
    }

    private void acknowledgeMessage(Message<?> message, boolean acknowledge) {
        BasicAcknowledgeablePubsubMessage originalMessage =
                message.getHeaders()
                        .get(
                                GcpPubSubHeaders.ORIGINAL_MESSAGE,
                                BasicAcknowledgeablePubsubMessage.class);
        if (originalMessage != null) {
            if (acknowledge) {
                log.debug("Acknowledging pubsub message");
                originalMessage.ack();
            } else {
                log.debug("N-Acknowledging pubsub message");
                originalMessage.nack();
            }
        }
    }

    private DocumentProcessingRequestWrapper unsafeParseRequest(Message<?> message) {
        try {
            var payload = (byte[]) message.getPayload();
            String requestWrapperString =
                    new String(payload, StandardCharsets.UTF_8); // convert byte[] to string
            return mapper.readValue(
                    requestWrapperString,
                    DocumentProcessingRequestWrapper.class); // parse the JSON string
        } catch (IOException ex) {
            log.error(ERROR_PARSING_MESSAGE, ex);
            throw new UncheckedIOException(ERROR_PARSING_MESSAGE, ex);
        }
    }
}
