package io.nuvalence.ds4g.documentmanagement.service.config;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import io.nuvalence.ds4g.documentmanagement.service.service.ClamAvSubscriber;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Configuration for the PubSub message receiver.
 */
@ConditionalOnProperty(name = "document-management.pubsub.enabled", havingValue = "true")
@Configuration
@Slf4j
@Getter
public class SubscribeClamAvConfig {

    private static final String INPUT_CHANNEL = "inputChannel";
    private final String subscriptionName;
    private final String topicName;
    private final boolean emulatedPubSub;

    private final ClamAvSubscriber subscriber;

    /**
     * Subscriber config constructor.
     * 
     * @param subscriptionName the name of the subscription for group pulling of messages
     * @param topicName       the name of the topic to pull messages from
     * @param emulatedPubSubString whether to use the emulated pubsub service (as string)
     * @param subscriber     the subscriber bean
     */
    public SubscribeClamAvConfig(
            @Value("${document-management.pubsub.clamav-subscription}") String subscriptionName,
            @Value("${document-management.pubsub.clamav-topic}") String topicName,
            @Value("${spring.cloud.gcp.pubsub.emulator-host:false}") String emulatedPubSubString,
            ClamAvSubscriber subscriber) {
        this.subscriptionName = subscriptionName;
        this.topicName = topicName;
        this.subscriber = subscriber;
        emulatedPubSub = !emulatedPubSubString.trim().equalsIgnoreCase("false");
    }

    // the spring message channel
    @Bean
    public MessageChannel inputChannel() {
        return new DirectChannel();
    }

    /**
     * Configures the message receiver, defining the subscription and the ack mode.
     * 
     * @param inputChannel internal spring message channel to pipe messages to
     * @param pubSubTemplate PubSub object enabling the pulling of data
     * @param admin PubSub admin object enabling the creation of topics and subscriptions only when emulated PubSub
     * @return Channel adapter for the message receiver
     */
    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(
            @Qualifier(INPUT_CHANNEL) MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate,
            PubSubAdmin admin) {

        // creating topic and subscription for emulated pubsub if they don't exist yet
        if (emulatedPubSub) {
            log.warn("Using emulated PubSub service.");
            // creating topic if it doesn't exist
            if (admin.getTopic(topicName) == null) {
                admin.createTopic(topicName);
            }

            // creating subscription if it doesn't exist
            if (admin.getSubscription(subscriptionName) == null) {
                admin.createSubscription(subscriptionName, topicName);
            }
        }

        // configuring subscription
        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        // sending to a spring message channel
        adapter.setOutputChannel(inputChannel);

        // configuring ack mode
        adapter.setAckMode(AckMode.MANUAL);

        return adapter;
    }

    // this activates the bean implementing the actual message-receive handling
    @Bean
    @ServiceActivator(inputChannel = INPUT_CHANNEL)
    public MessageHandler messageReceiver() {
        return subscriber;
    }
}
