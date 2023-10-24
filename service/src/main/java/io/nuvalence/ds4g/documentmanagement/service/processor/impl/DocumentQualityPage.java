package io.nuvalence.ds4g.documentmanagement.service.processor.impl;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a page of a document with quality information.
 */
@Data
@NoArgsConstructor
public class DocumentQualityPage {
    private int pageNumber;
    private float qualityScore;
    private List<DocumentQualityDefect> defects;

    /**
     * Builder for DocumentQualityPage.
     *
     * @param pageNumber number of the evaluated page.
     * @param qualityScore score obtained.
     * @param defects found defects
     */
    @Builder
    public DocumentQualityPage(
            int pageNumber, float qualityScore, List<DocumentQualityDefect> defects) {
        this.pageNumber = pageNumber;
        this.qualityScore = qualityScore;
        this.defects = defects;
    }
}
