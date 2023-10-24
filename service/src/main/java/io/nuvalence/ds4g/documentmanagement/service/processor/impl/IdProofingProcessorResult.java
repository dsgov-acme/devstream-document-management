package io.nuvalence.ds4g.documentmanagement.service.processor.impl;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Custom processing result for IdProofingProcessor processor.
 */
@Data
@NoArgsConstructor
public class IdProofingProcessorResult {
    private boolean allPass;
    private List<IdFraudSignal> signals;

    @Builder
    public IdProofingProcessorResult(boolean allPass, List<IdFraudSignal> signals) {
        this.allPass = allPass;
        this.signals = signals;
    }
}
