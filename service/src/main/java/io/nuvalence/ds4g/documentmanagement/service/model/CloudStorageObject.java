package io.nuvalence.ds4g.documentmanagement.service.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Represents a Cloud Storage object, as represented in a PubSub message from the Cloud Storage bucket.
 */
@Getter
@Setter
public class CloudStorageObject {
    private String kind;
    private String id;
    private String selfLink;
    private String name;
    private String bucket;
    private String generation;
    private String metageneration;
    private String contentType;
    private String timeCreated;
    private String updated;
    private String storageClass;
    private String timeStorageClassUpdated;
    private String size;
    private String md5Hash;
    private String mediaLink;
    private Map<String, String> metadata;
    private String crc32c;
    private String etag;
}
