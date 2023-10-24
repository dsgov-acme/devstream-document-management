package io.nuvalence.ds4g.documentmanagement.service.service;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import io.nuvalence.ds4g.documentmanagement.service.antivirus.AntivirusProvider.GeneralScanResult;
import io.nuvalence.ds4g.documentmanagement.service.antivirus.ClamAvScanner;
import io.nuvalence.ds4g.documentmanagement.service.model.CloudStorageObject;
import io.nuvalence.ds4g.documentmanagement.service.model.FileContent;
import io.nuvalence.ds4g.documentmanagement.service.storage.GoogleCloudStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

class ClamAvSubscriberTest {

    @Mock ClamAvScanner clamAvScanner;

    @Mock GoogleCloudStorage googleCloudStorage;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void handleCleanFileMessage() throws Exception {

        CloudStorageObject cloudStorageObject = new CloudStorageObject();

        cloudStorageObject.setBucket("bucketName");

        cloudStorageObject.setName("name");

        byte[] bytes = objectMapper.writeValueAsBytes(cloudStorageObject);

        InputStream bytesInputStream = new ByteArrayInputStream(bytes);

        FileContent fileContent = mock(FileContent.class);

        Resource resource = mock(Resource.class);

        when(googleCloudStorage.getUnscannedFileData(cloudStorageObject.getName()))
                .thenReturn(fileContent);

        when(fileContent.getContent()).thenReturn(resource);

        when(resource.getInputStream()).thenReturn(bytesInputStream);

        when(clamAvScanner.scan(bytesInputStream, cloudStorageObject.getName()))
                .thenReturn(new GeneralScanResult(true, "Clean"));

        BasicAcknowledgeablePubsubMessage originalMessage =
                mock(BasicAcknowledgeablePubsubMessage.class);

        Message<byte[]> message =
                MessageBuilder.withPayload(bytes)
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, originalMessage)
                        .build();

        ClamAvSubscriber subscriber =
                new ClamAvSubscriber("bucketName", clamAvScanner, googleCloudStorage, objectMapper);

        subscriber.handleMessage(message);

        verify(googleCloudStorage, times(1)).confirmCleanFile(cloudStorageObject.getName());
        verify(googleCloudStorage, times(0)).quaranfineFile(cloudStorageObject.getName());
        verify(clamAvScanner, times(1)).scan(bytesInputStream, cloudStorageObject.getName());
        verify(originalMessage, times(1)).ack();
    }

    @Test
    void handleCleanFileMessageInvalidBucket() throws Exception {

        CloudStorageObject cloudStorageObject = mock(CloudStorageObject.class);

        when(cloudStorageObject.getBucket()).thenReturn("invalidBucketName");

        byte[] bytes = objectMapper.writeValueAsBytes(cloudStorageObject);

        InputStream bytesInputStream = new ByteArrayInputStream(bytes);

        FileContent fileContent = mock(FileContent.class);

        Resource resource = mock(Resource.class);

        when(googleCloudStorage.getUnscannedFileData(cloudStorageObject.getName()))
                .thenReturn(fileContent);

        when(fileContent.getContent()).thenReturn(resource);

        when(resource.getInputStream()).thenReturn(bytesInputStream);

        when(clamAvScanner.scan(bytesInputStream, cloudStorageObject.getName()))
                .thenReturn(new GeneralScanResult(true, "Clean"));

        BasicAcknowledgeablePubsubMessage originalMessage =
                mock(BasicAcknowledgeablePubsubMessage.class);

        Message<byte[]> message =
                MessageBuilder.withPayload(bytes)
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, originalMessage)
                        .build();

        ClamAvSubscriber subscriber =
                new ClamAvSubscriber("bucketName", clamAvScanner, googleCloudStorage, objectMapper);

        subscriber.handleMessage(message);

        verify(googleCloudStorage, times(0)).confirmCleanFile(cloudStorageObject.getName());
        verify(googleCloudStorage, times(0)).quaranfineFile(cloudStorageObject.getName());
        verify(clamAvScanner, times(0)).scan(bytesInputStream, cloudStorageObject.getName());
        verify(originalMessage, times(1)).ack();
    }

    @Test
    void handleInfectedFileMessage() throws Exception {

        CloudStorageObject cloudStorageObject = new CloudStorageObject();

        cloudStorageObject.setBucket("bucketName");

        cloudStorageObject.setName("name");

        byte[] bytes = objectMapper.writeValueAsBytes(cloudStorageObject);

        InputStream bytesInputStream = new ByteArrayInputStream(bytes);

        FileContent fileContent = mock(FileContent.class);

        Resource resource = mock(Resource.class);

        when(googleCloudStorage.getUnscannedFileData(cloudStorageObject.getName()))
                .thenReturn(fileContent);

        when(fileContent.getContent()).thenReturn(resource);

        when(resource.getInputStream()).thenReturn(bytesInputStream);

        when(clamAvScanner.scan(bytesInputStream, cloudStorageObject.getName()))
                .thenReturn(new GeneralScanResult(false, "Infected file"));

        BasicAcknowledgeablePubsubMessage originalMessage =
                mock(BasicAcknowledgeablePubsubMessage.class);

        Message<byte[]> message =
                MessageBuilder.withPayload(bytes)
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, originalMessage)
                        .build();

        ClamAvSubscriber subscriber =
                new ClamAvSubscriber("bucketName", clamAvScanner, googleCloudStorage, objectMapper);

        subscriber.handleMessage(message);

        verify(googleCloudStorage, times(0)).confirmCleanFile(cloudStorageObject.getName());
        verify(googleCloudStorage, times(1)).quaranfineFile(cloudStorageObject.getName());
        verify(clamAvScanner, times(1)).scan(bytesInputStream, cloudStorageObject.getName());
        verify(originalMessage, times(1)).ack();
    }

    @Test
    void handleCloudStorageException() throws Exception {

        CloudStorageObject cloudStorageObject = new CloudStorageObject();

        cloudStorageObject.setBucket("bucketName");

        cloudStorageObject.setName("name");

        byte[] bytes = objectMapper.writeValueAsBytes(cloudStorageObject);

        InputStream bytesInputStream = new ByteArrayInputStream(bytes);

        FileContent fileContent = mock(FileContent.class);

        Resource resource = mock(Resource.class);

        when(googleCloudStorage.getUnscannedFileData(cloudStorageObject.getName()))
                .thenReturn(fileContent);

        when(fileContent.getContent()).thenReturn(resource);

        when(resource.getInputStream()).thenReturn(bytesInputStream);

        when(clamAvScanner.scan(bytesInputStream, cloudStorageObject.getName()))
                .thenReturn(new GeneralScanResult(true, "Clean"));

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Already Scanned"))
                .when(googleCloudStorage)
                .confirmCleanFile(cloudStorageObject.getName());

        BasicAcknowledgeablePubsubMessage originalMessage =
                mock(BasicAcknowledgeablePubsubMessage.class);

        Message<byte[]> message =
                MessageBuilder.withPayload(bytes)
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, originalMessage)
                        .build();

        ClamAvSubscriber subscriber =
                new ClamAvSubscriber("bucketName", clamAvScanner, googleCloudStorage, objectMapper);

        subscriber.handleMessage(message);

        verify(googleCloudStorage, times(1)).confirmCleanFile(cloudStorageObject.getName());
        verify(googleCloudStorage, times(0)).quaranfineFile(cloudStorageObject.getName());
        verify(clamAvScanner, times(1)).scan(bytesInputStream, cloudStorageObject.getName());
        verify(originalMessage, times(1)).ack();
    }
}
