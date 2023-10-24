package io.nuvalence.ds4g.documentmanagement.service.processor.impl;

import com.google.cloud.documentai.v1beta3.Document;
import com.google.cloud.documentai.v1beta3.ProcessResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation Id-Proofing Processor.
 */
@Service
public class IdProofingProcessor extends DocumentAiProcessor<IdProofingProcessorResult> {

    private static final String PROCESSOR_ID = "docai-id-proofing";
    private static final String PASS = "PASS";

    public IdProofingProcessor(
            @Value("${document-management.processor.document-id-proofing-processor-id}")
                    String processorId)
            throws IOException {
        super.createClient(processorId);
    }

    @Override
    String getProcessorName() {
        return null;
    }

    @Override
    IdProofingProcessorResult parseResults(ProcessResponse response) {
        IdProofingProcessorResult idProofingResponse = null;
        if (response.hasDocument()) {
            boolean allPass = true;
            List<IdFraudSignal> signals = new ArrayList<>();
            List<Document.Entity> entities = response.getDocument().getEntitiesList();
            for (Document.Entity entity : entities) {
                boolean pass = PASS.equals(entity.getMentionText());
                if (!pass) {
                    allPass = false;
                }
                String type = entity.getType();
                signals.add(new IdFraudSignal(type, pass, entity.getMentionText()));
            }

            idProofingResponse = new IdProofingProcessorResult(allPass, signals);
        }
        return idProofingResponse;
    }

    @Override
    public String getProcessorId() {
        return PROCESSOR_ID;
    }
}
