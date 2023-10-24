package io.nuvalence.ds4g.documentmanagement.service.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.nuvalence.auth.access.AccessResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a single Document Processor Result Entity.
 *
 * @param <T> Type of results
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "document_processor_result")
@AccessResource("document_processor_result")
@IdClass(DocumentProcessingResultId.class)
public class DocumentProcessorResult<T> implements Serializable {
    private static final long serialVersionUID = 1013121827840528434L;

    @Id
    @Column(name = "processor_id", nullable = false)
    private String processorId;

    @Id
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentProcessorStatus status;

    @Setter
    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Type(JsonType.class)
    @Column(name = "result", nullable = false, columnDefinition = "json")
    private T result;
}
