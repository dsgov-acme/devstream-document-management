package io.nuvalence.ds4g.documentmanagement.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import io.nuvalence.ds4g.documentmanagement.service.antivirus.AntivirusException;
import io.nuvalence.ds4g.documentmanagement.service.antivirus.AntivirusProvider;
import io.nuvalence.ds4g.documentmanagement.service.model.CloudStorageObject;
import io.nuvalence.ds4g.documentmanagement.service.model.FileContent;
import io.nuvalence.ds4g.documentmanagement.service.storage.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Service to handle messages from the PubSub subscription.
 */
@ConditionalOnProperty(name = "document-management.pubsub.enabled", havingValue = "true")
@Service
@Slf4j
public class ClamAvSubscriber implements MessageHandler {

    private final String bucketToListen;
    private final AntivirusProvider antivirus;
    private final StorageProvider storage;
    private final ObjectMapper mapper;

    /**
     * Subscriber constructor.
     * 
     * @param bucketToListen the name of the bucket to listen for messages
     * @param antivirus      the antivirus provider bean
     * @param storage        the storage provider bean
     * @param mapper         the object mapper bean
     */
    public ClamAvSubscriber(
            @Value("${document-management.google.bucket.unscanned-files}") String bucketToListen,
            AntivirusProvider antivirus,
            StorageProvider storage,
            ObjectMapper mapper) {
        this.bucketToListen = bucketToListen;
        this.antivirus = antivirus;
        this.storage = storage;
        this.mapper = mapper;
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {

        CloudStorageObject storageObject;
        try {
            storageObject =
                    mapper.readValue(
                            new String((byte[]) message.getPayload(), StandardCharsets.UTF_8),
                            CloudStorageObject.class);

            if (!storageObject.getBucket().equals(bucketToListen)) {
                log.debug("Ignoring message from bucket {}", storageObject.getBucket());
                acknowledgeMessage(message);
                return;
            }

            var storageObjectName = storageObject.getName();

            processScannning(storageObjectName);

            acknowledgeMessage(message);
        } catch (ResponseStatusException e) {
            // ack messages for files already scanned or quarantined or not found
            log.warn(
                    "Error getting file data for malware scan: {}. {}",
                    e.getStatusCode(),
                    e.getReason());
            acknowledgeMessage(message);
        } catch (JsonProcessingException e) {
            throw new MessagingException("Error parsing message payload", e);
        } catch (AntivirusException e) {
            throw new MessagingException("Error scanning file", e);
        } catch (IOException e) {
            throw new MessagingException("Error processing message", e);
        }
    }

    private void processScannning(String storageObjectName) throws IOException {
        log.debug("Scanning file: {}", storageObjectName);

        FileContent fileData = storage.getUnscannedFileData(storageObjectName);
        try (InputStream dataInputStream = fileData.getContent().getInputStream()) {

            var antivirusResult = antivirus.scan(dataInputStream, storageObjectName);

            if (antivirusResult.isClean()) {
                log.debug("File {} is clean", storageObjectName);
                storage.confirmCleanFile(storageObjectName);
            } else {
                log.warn("File {} is infected", storageObjectName);
                storage.quaranfineFile(storageObjectName);
            }
        }
    }

    private void acknowledgeMessage(Message<?> message) {
        BasicAcknowledgeablePubsubMessage originalMessage =
                message.getHeaders()
                        .get(
                                GcpPubSubHeaders.ORIGINAL_MESSAGE,
                                BasicAcknowledgeablePubsubMessage.class);
        if (originalMessage != null) {
            log.debug("Acknowledging pubsub message");
            originalMessage.ack();
        }
    }
}
