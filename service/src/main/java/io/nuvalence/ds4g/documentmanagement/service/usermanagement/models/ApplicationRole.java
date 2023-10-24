package io.nuvalence.ds4g.documentmanagement.service.usermanagement.models;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents an individual application role.
 */
@Getter
@Builder
@ToString
@Jacksonized
@SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
public class ApplicationRole {
    private String applicationRole;
    private String name;
    private String description;
    private String group;
}
