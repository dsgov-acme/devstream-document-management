package io.nuvalence.ds4g.documentmanagement.service.processor;

import io.nuvalence.ds4g.documentmanagement.service.generated.models.DocumentProcessingRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  Wrapper class for processing results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingRequestWrapper {
    private DocumentProcessingRequest request;
    private String documentId;
}
