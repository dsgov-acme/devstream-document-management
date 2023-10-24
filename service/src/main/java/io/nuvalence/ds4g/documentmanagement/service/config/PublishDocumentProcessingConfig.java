package io.nuvalence.ds4g.documentmanagement.service.config;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;

/**
 * Publisher for document processing requests.
 */
@Configuration
public class PublishDocumentProcessingConfig {

    private static final String OUTPUT_CHANNEL = "outputChannelDocumentProcessing";

    private final String topicName;

    private final boolean createTopicAndSubs;

    /**
     * Constructor for this class.
     *
     * @param topicName topic to publish to.
     * @param createTopicAndSubs authorization for dynamically creating topics.
     */
    public PublishDocumentProcessingConfig(
            @Value("${document-management.pubsub.document-processing-topic}") String topicName,
            @Value("${document-management.gcloud-storage-emulator.enabled:false}")
                    boolean createTopicAndSubs) {
        this.topicName = topicName;
        this.createTopicAndSubs = createTopicAndSubs;
    }

    /**
     * Constructor for this class.
     *
     * @param pubsubTemplate template for creating handler.
     * @param admin PubSub admin.
     * @return a message handler. 
     */
    @Bean
    @ServiceActivator(inputChannel = OUTPUT_CHANNEL)
    public MessageHandler messageProcessingSender(
            PubSubTemplate pubsubTemplate, PubSubAdmin admin) {

        // creating topic if it does not exist
        if (createTopicAndSubs && (admin.getTopic(topicName) == null)) {
            admin.createTopic(topicName);
        }

        // configuring topic
        return new PubSubMessageHandler(pubsubTemplate, topicName);
    }

    /**
     * Interface for publishing processing requests.
     */
    @MessagingGateway(defaultRequestChannel = OUTPUT_CHANNEL)
    public interface DocumentProcessingPublisher {
        void publish(String wrapperString);
    }
}
