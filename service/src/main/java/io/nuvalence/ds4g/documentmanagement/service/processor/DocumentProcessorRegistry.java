package io.nuvalence.ds4g.documentmanagement.service.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *  Registry for document processors.
 */
@Service
@Slf4j
public class DocumentProcessorRegistry {

    private final Map<String, DocumentProcessor> processors = new HashMap<>();

    /**
     * Constructor.
     * @param documentProcessors list of document processors
     */
    public DocumentProcessorRegistry(List<DocumentProcessor> documentProcessors) {
        log.info("Size Registering processors {}", documentProcessors.size());
        for (DocumentProcessor d : documentProcessors) {
            log.info("Registering processor: {}", d.getProcessorId());
            processors.put(d.getProcessorId(), d);
        }
    }

    /**
     * Get Document Processor by processor id.
     * @param processorId processor id
     * @return Document Processor
     */
    public Optional<DocumentProcessor> getProcessor(String processorId) {
        return Optional.ofNullable(processors.get(processorId));
    }
}
