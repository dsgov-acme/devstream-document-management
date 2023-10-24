package io.nuvalence.ds4g.documentmanagement.service.processor.impl;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a quality defect from an Document Quality  processor.
 */
@Data
@NoArgsConstructor
public class DocumentQualityDefect {
    private String name;
    private float confidence;
    private int pageNumber;

    /**
     * Builder for DocumentQualityDefect.
     *
     * @param pageNumber number of the evaluated page.
     * @param confidence confidence score.
     * @param name of defect.
     */
    @Builder
    public DocumentQualityDefect(String name, float confidence, int pageNumber) {
        this.name = name;
        this.confidence = confidence;
        this.pageNumber = pageNumber;
    }
}
