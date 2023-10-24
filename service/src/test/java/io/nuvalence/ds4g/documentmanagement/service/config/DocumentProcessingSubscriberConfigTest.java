package io.nuvalence.ds4g.documentmanagement.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.pubsub.v1.Subscription;
import io.nuvalence.ds4g.documentmanagement.service.service.DocumentProcessingSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.MessageChannel;

class DocumentProcessingSubscriberConfigTest {
    @Test
    void simpleConfigForEmulatedPubSub() throws Exception {

        String topic = "topic";
        String subscription = "subscription";
        String host = "localhost:8085";
        String deadLetterTopic = "deadLetterTopic";

        DocumentProcessingSubscriber subs = mock(DocumentProcessingSubscriber.class);

        SubscribeDocumentProcessingConfig subconf =
                new SubscribeDocumentProcessingConfig(
                        subscription, topic, host, deadLetterTopic, subs);

        PubSubTemplate templateMock = mock(PubSubTemplate.class);
        try (PubSubAdmin adminMock = mock(PubSubAdmin.class)) {

            // verify that the config is set to emulate pubsub when the host is a value
            // different than false
            assert (subconf.isEmulatedPubSub());

            MessageChannel messageChannel = subconf.inputChannelDocumentProcessing();
            PubSubInboundChannelAdapter adapter =
                    subconf.messageChannelAdapterDocumentProcessing(
                            messageChannel, templateMock, adminMock);

            // proper config verifications
            assert (adapter.getAckMode().equals(AckMode.MANUAL));
            assert (adapter.getOutputChannel().equals(messageChannel));
            verify(adminMock, times(1)).getTopic(topic);
            verify(adminMock, times(1)).createTopic(topic);
            verify(adminMock, times(1)).getSubscription(subscription);
            ArgumentCaptor<Subscription.Builder> captor =
                    ArgumentCaptor.forClass(Subscription.Builder.class);
            verify(adminMock, times(1)).createSubscription(captor.capture());
            Subscription.Builder capturedArgument = captor.getValue();
            assertEquals(subscription, capturedArgument.getName());
            assertEquals(topic, capturedArgument.getTopic());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void simpleConfigForEmulatedPubSubFalse() {

        DocumentProcessingSubscriber subs = mock(DocumentProcessingSubscriber.class);

        SubscribeDocumentProcessingConfig subconf =
                new SubscribeDocumentProcessingConfig(
                        "subscription", "topic", "false", "deadLetterTopic", subs);

        Assertions.assertFalse(subconf.isEmulatedPubSub());
    }
}
