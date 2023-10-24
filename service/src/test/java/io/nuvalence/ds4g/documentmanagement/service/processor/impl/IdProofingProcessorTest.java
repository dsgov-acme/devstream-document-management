package io.nuvalence.ds4g.documentmanagement.service.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.cloud.documentai.v1beta3.Document;
import com.google.cloud.documentai.v1beta3.ProcessResponse;
import io.nuvalence.ds4g.documentmanagement.service.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;

class IdProofingProcessorTest {
    @Mock private StorageProvider storage;

    @Test
    void testParseResults() throws IOException {
        Document document =
                Document.newBuilder()
                        .addEntities(
                                Document.Entity.newBuilder()
                                        .setType("fraud_signals_is_identity_document")
                                        .setMentionText("PASS")
                                        .build())
                        .addEntities(
                                Document.Entity.newBuilder()
                                        .setType("fraud_signals_suspicious_words")
                                        .setMentionText("SUSPICIOUS_WORDS_FOUND")
                                        .build())
                        .addEntities(
                                Document.Entity.newBuilder()
                                        .setType("evidence_suspicious_word")
                                        .setMentionText("BERTHIER CORINNE")
                                        .build())
                        .addEntities(
                                Document.Entity.newBuilder()
                                        .setType("fraud_signals_suspicious_words")
                                        .setMentionText("INCONCLUSIVE")
                                        .build())
                        .addEntities(
                                Document.Entity.newBuilder()
                                        .setType("evidence_inconclusive_suspicious_word")
                                        .setMentionText("tools")
                                        .build())
                        .addEntities(
                                Document.Entity.newBuilder()
                                        .setType("fraud_signals_image_manipulation")
                                        .setMentionText("PASS")
                                        .build())
                        .addEntities(
                                Document.Entity.newBuilder()
                                        .setType("fraud_signals_image_manipulation")
                                        .setMentionText("POSSIBLE_IMAGE_MANIPULATION")
                                        .build())
                        .addEntities(
                                Document.Entity.newBuilder()
                                        .setType("evidence_thumbnail_url")
                                        .setMentionText(
                                                "https://encrypted-tbn2.gstatic.com/images?q=tbn:ANd9GcTAQMlGKTWhM-wXVtRtJiVXoi-k1S5QVr1L4snYnNWRx5sLju6A")
                                        .build())
                        .addEntities(
                                Document.Entity.newBuilder()
                                        .setType("evidence_hostname")
                                        .setMentionText("www.dhnet.be")
                                        .build())
                        .addEntities(
                                Document.Entity.newBuilder()
                                        .setType("fraud_signals_online_duplicate")
                                        .setMentionText("POSSIBLE_ONLINE_DUPLICATE")
                                        .build())
                        .addEntities(
                                Document.Entity.newBuilder()
                                        .setType("evidence_thumbnail_url")
                                        .setMentionText(
                                                "https://encrypted-tbn2.gstatic.com/images?q=tbn:ANd9GcRufiAFkrTKhgj74v-9_nfq6d8MjV9jqfgOAtKeLQNAlDWrLJB5")
                                        .build())
                        .build();
        ProcessResponse response = ProcessResponse.newBuilder().setDocument(document).build();

        IdProofingProcessor processor = new IdProofingProcessor("processorId");
        processor.setStorage(storage);

        IdProofingProcessorResult result = processor.parseResults(response);
        assertFalse(result.isAllPass());
        assertEquals(document.getEntitiesList().size(), result.getSignals().size());
    }
}
