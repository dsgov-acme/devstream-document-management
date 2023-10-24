package io.nuvalence.ds4g.documentmanagement.service.config;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingResultPublisherTest {
    @Mock PublishDocumentProcessingResultConfig.DocumentProcessingResultPublisher publisher;

    @Test
    void testPublish() {

        String testString = "Test string";

        publisher.publish(testString);

        verify(publisher, times(1)).publish(testString);
    }
}
