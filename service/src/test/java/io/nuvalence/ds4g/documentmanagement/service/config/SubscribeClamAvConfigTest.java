package io.nuvalence.ds4g.documentmanagement.service.config;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.nuvalence.ds4g.documentmanagement.service.service.ClamAvSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

class SubscribeClamAvConfigTest {

    @Test
    void simpleConfigForEmulatedPubSub() {

        String topic = "topic";
        String subscription = "subscription";
        String host = "localhost:8085";

        ClamAvSubscriber subs = mock(ClamAvSubscriber.class);

        SubscribeClamAvConfig subconf = new SubscribeClamAvConfig(subscription, topic, host, subs);

        PubSubTemplate templateMock = mock(PubSubTemplate.class);
        try (PubSubAdmin adminMock = mock(PubSubAdmin.class)) {

            // verify that the config is set to emulate pubsub when the host is a value
            // different than false
            assert (subconf.isEmulatedPubSub());

            MessageChannel messageChannel = subconf.inputChannel();
            PubSubInboundChannelAdapter adapter =
                    subconf.messageChannelAdapter(messageChannel, templateMock, adminMock);

            // proper config verifications
            assert (adapter.getAckMode().equals(AckMode.MANUAL));
            assert (adapter.getOutputChannel().equals(messageChannel));
            verify(adminMock, times(1)).getTopic(topic);
            verify(adminMock, times(1)).createTopic(topic);
            verify(adminMock, times(1)).getSubscription(subscription);
            verify(adminMock, times(1)).createSubscription(subscription, topic);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void simpleConfigForEmulatedPubSubFalse() {

        ClamAvSubscriber subs = mock(ClamAvSubscriber.class);

        SubscribeClamAvConfig subconf =
                new SubscribeClamAvConfig("subscription", "topic", "false", subs);

        Assertions.assertFalse(subconf.isEmulatedPubSub());
    }

    @Test
    void emulatedStorageBucketsTest() {

        var storageOptionsStatic = Mockito.mockStatic(StorageOptions.class);
        var bucketInfoStatic = Mockito.mockStatic(BucketInfo.class);
        StorageOptions.Builder builder = mock(StorageOptions.Builder.class);
        StorageOptions storageOptionsInstance = mock(StorageOptions.class);
        try (Storage storage = mock(Storage.class)) {
            BucketInfo bucketInfo = mock(BucketInfo.class);
            BucketInfo.Builder bucketInfoBuilder = mock(BucketInfo.Builder.class);

            when(builder.setHost(anyString())).thenReturn(builder);
            when(builder.setCredentials(any())).thenReturn(builder);
            when(builder.build()).thenReturn(storageOptionsInstance);
            when(storageOptionsInstance.getService()).thenReturn(storage);
            when(bucketInfoBuilder.build()).thenReturn(bucketInfo);
            storageOptionsStatic.when(() -> StorageOptions.newBuilder()).thenReturn(builder);
            bucketInfoStatic.when(() -> BucketInfo.newBuilder(any())).thenReturn(bucketInfoBuilder);

            when(storage.create(any(BucketInfo.class))).thenReturn(null);

            Configuration config = new Configuration();

            // method to test
            config.getGoogleCloudStorageEmulator(
                    "unscanned", "quarantined", "scanned", "localhost:4443");

            // Querying and creating 3 buckets one for each type
            verify(storage, times(3)).get(anyString());
            verify(storage, times(3)).create(any(BucketInfo.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void messageReceiver_ValidMessage_SubscriberExists() {
        String topic = "topic";
        String subscription = "subscription";
        String host = "localhost:8085";
        // Arrange
        ClamAvSubscriber subs = mock(ClamAvSubscriber.class);
        SubscribeClamAvConfig subconf = new SubscribeClamAvConfig(subscription, topic, host, subs);
        MessageHandler messageHandler = subconf.messageReceiver();

        // Assert
        Assertions.assertNotNull(messageHandler);
    }
}
