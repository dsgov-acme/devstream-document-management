package io.nuvalence.ds4g.documentmanagement.service.processor.impl;

import com.google.cloud.documentai.v1beta3.Document;
import com.google.cloud.documentai.v1beta3.OcrConfig;
import com.google.cloud.documentai.v1beta3.ProcessOptions;
import com.google.cloud.documentai.v1beta3.ProcessResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation Document Quality Processor..
 */
@Service
public class DocumentQualityProcessor extends DocumentAiProcessor<DocumentQualityProcessorResult> {

    private static final String PROCESSOR_ID = "docai-document-quality";

    public DocumentQualityProcessor(
            @Value("${document-management.processor.document-quality-processor-id}")
                    String processorId)
            throws IOException {
        super.createClient(processorId);
    }

    @Override
    public String getProcessorId() {
        return PROCESSOR_ID;
    }

    @Override
    String getProcessorName() {
        return null;
    }

    @Override
    protected ProcessOptions addProcessorOptions() {
        return ProcessOptions.newBuilder()
                .setOcrConfig(OcrConfig.newBuilder().setEnableImageQualityScores(true).build())
                .build();
    }

    @Override
    DocumentQualityProcessorResult parseResults(ProcessResponse response) {
        DocumentQualityProcessorResult qualityResult = null;
        List<DocumentQualityPage> pages = new ArrayList<>();
        if (response.hasDocument()) {
            List<Document.Page> documentPages = response.getDocument().getPagesList();
            float qualityScore = 1;
            for (Document.Page documentPage : documentPages) {
                List<DocumentQualityDefect> defects = new ArrayList<>();
                if (qualityScore > documentPage.getImageQualityScores().getQualityScore()) {
                    qualityScore = documentPage.getImageQualityScores().getQualityScore();
                }
                List<Document.Page.ImageQualityScores.DetectedDefect> detectedDefects =
                        documentPage.getImageQualityScores().getDetectedDefectsList();
                detectedDefects.forEach(
                        detectedDefect ->
                                defects.add(
                                        new DocumentQualityDefect(
                                                detectedDefect.getType(),
                                                detectedDefect.getConfidence(),
                                                documentPage.getPageNumber())));
                pages.add(
                        new DocumentQualityPage(
                                documentPage.getPageNumber(),
                                documentPage.getImageQualityScores().getQualityScore(),
                                defects));
            }
            qualityResult = new DocumentQualityProcessorResult(qualityScore, pages);
        }
        return qualityResult;
    }
}
