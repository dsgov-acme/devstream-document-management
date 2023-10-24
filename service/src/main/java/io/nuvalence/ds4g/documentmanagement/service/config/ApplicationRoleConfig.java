package io.nuvalence.ds4g.documentmanagement.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.ds4g.documentmanagement.service.usermanagement.UserManagementClient;
import io.nuvalence.ds4g.documentmanagement.service.usermanagement.models.ApplicationRoles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.NoSuchElementException;

/**
 * ApplicationRoleConfig class.
 */
@Component
@Slf4j
public class ApplicationRoleConfig {

    private static final String CONFIG = "roles.json";
    private final UserManagementClient client;

    public ApplicationRoleConfig(UserManagementClient client) {
        this.client = client;
    }

    /**
     * Publishes roles to User Management from JSON file.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void publishRoles() {
        try {
            // Load JSON payload from file
            final Resource rolesResource = new ClassPathResource(CONFIG);

            if (!rolesResource.exists()) {
                throw new NoSuchElementException("Role configuration file does not exist.");
            }

            try (final InputStream fileStream = rolesResource.getInputStream()) {
                ApplicationRoles roles =
                        new ObjectMapper().readValue(fileStream, ApplicationRoles.class);

                this.client.publishRoles(roles);
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
