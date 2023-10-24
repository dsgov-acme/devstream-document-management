package io.nuvalence.ds4g.documentmanagement.service.repository;

import io.nuvalence.ds4g.documentmanagement.service.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 *  Repository for Document.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {}
