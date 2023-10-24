package io.nuvalence.ds4g.documentmanagement.service.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import io.nuvalence.ds4g.documentmanagement.service.service.DocumentProcessingService;
import io.nuvalence.ds4g.documentmanagement.service.service.DocumentProcessingSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHandler;

@ExtendWith(MockitoExtension.class)
class PublishDocumentProcessingConfigTest {
    @Mock private PubSubTemplate pubSubTemplate;

    @Mock private PubSubAdmin pubSubAdmin;

    @Mock private ObjectMapper objectMapper;

    @Mock private DocumentProcessingService documentProcessingService;

    @Test
    void testMessageResultSender() {
        PublishDocumentProcessingConfig documentProcessingResultConfig =
                new PublishDocumentProcessingConfig("testTopic", true);

        when(pubSubAdmin.getTopic("testTopic")).thenReturn(null);

        MessageHandler handler =
                documentProcessingResultConfig.messageProcessingSender(pubSubTemplate, pubSubAdmin);

        verify(pubSubAdmin, times(1)).createTopic("testTopic");
        assertNotNull(handler);
    }

    @Test
    void messageReceiver_ValidMessage_SubscriberExists() {
        String topic = "topic";
        String subscription = "subscription";
        String host = "localhost:8085";
        // Arrange
        DocumentProcessingSubscriber subs = mock(DocumentProcessingSubscriber.class);
        SubscribeDocumentProcessingConfig subconf =
                new SubscribeDocumentProcessingConfig(
                        subscription, topic, host, "results-dead-letter-topic", subs);
        MessageHandler messageHandler = subconf.messageReceiverDocumentProcessing();

        // Assert
        Assertions.assertNotNull(messageHandler);
    }
}
