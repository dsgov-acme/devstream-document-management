package io.nuvalence.ds4g.documentmanagement.service.model;

import io.nuvalence.auth.access.AccessResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * File download model of a document.
 */
@AllArgsConstructor
@Builder
@Getter
@AccessResource("document")
public class FileContent {
    private final Resource content;
    private final MediaType contentType;
}
