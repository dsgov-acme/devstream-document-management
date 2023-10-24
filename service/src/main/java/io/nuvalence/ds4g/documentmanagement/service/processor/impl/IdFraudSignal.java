package io.nuvalence.ds4g.documentmanagement.service.processor.impl;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a fraud signal from an ID proofing processor.
 */
@Data
@NoArgsConstructor
public class IdFraudSignal {
    private String name;
    private Boolean isPass;
    private String mentionText;

    /**
     * Builder for IdFraudSignal.
     *
     * @param name name of the signal.
     * @param isPass is deemed fraud or not
     * @param mentionText text
     */
    @Builder
    public IdFraudSignal(String name, Boolean isPass, String mentionText) {
        this.name = name;
        this.isPass = isPass;
        this.mentionText = mentionText;
    }
}
