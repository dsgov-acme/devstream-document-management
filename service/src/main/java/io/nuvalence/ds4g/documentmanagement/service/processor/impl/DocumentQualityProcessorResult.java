package io.nuvalence.ds4g.documentmanagement.service.processor.impl;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Interface for document processors.
 */
@Data
@NoArgsConstructor
public class DocumentQualityProcessorResult {
    private float qualityScore;
    private List<DocumentQualityPage> pages;

    @Builder
    public DocumentQualityProcessorResult(float qualityScore, List<DocumentQualityPage> pages) {
        this.qualityScore = qualityScore;
        this.pages = pages;
    }
}
