package io.nuvalence.ds4g.documentmanagement.service.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHandler;

@ExtendWith(MockitoExtension.class)
class PublishDocumentProcessingResultConfigTest {
    @Mock private PubSubTemplate pubSubTemplate;

    @Mock private PubSubAdmin pubSubAdmin;

    @Test
    void testMessageResultSender() {
        PublishDocumentProcessingResultConfig documentProcessingResultConfig =
                new PublishDocumentProcessingResultConfig("testTopic", true);

        when(pubSubAdmin.getTopic("testTopic")).thenReturn(null);

        MessageHandler handler =
                documentProcessingResultConfig.messageResultSender(pubSubTemplate, pubSubAdmin);

        verify(pubSubAdmin, times(1)).createTopic("testTopic");
        assertNotNull(handler);
    }
}
