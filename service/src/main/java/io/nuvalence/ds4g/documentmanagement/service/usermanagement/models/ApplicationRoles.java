package io.nuvalence.ds4g.documentmanagement.service.usermanagement.models;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Represents a list of roles that this application uses.
 */
@Getter
@Builder
@ToString
@Jacksonized
public class ApplicationRoles {
    private String name;
    private List<ApplicationRole> roles;
}
