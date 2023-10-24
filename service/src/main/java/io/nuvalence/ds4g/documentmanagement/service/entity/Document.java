package io.nuvalence.ds4g.documentmanagement.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Represents a Document Entity.
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "document")
public class Document {

    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    private UUID id;

    @Column(name = "filename", nullable = false)
    private String filename;

    @OneToMany
    @JoinColumn(name = "document_id")
    private List<DocumentProcessorResult> processingResults;

    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;
}
