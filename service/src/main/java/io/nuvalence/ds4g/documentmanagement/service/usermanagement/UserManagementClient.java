package io.nuvalence.ds4g.documentmanagement.service.usermanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.ds4g.documentmanagement.service.usermanagement.models.ApplicationRoles;
import io.nuvalence.ds4g.documentmanagement.service.util.ServiceTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Interfaces with the User Management service.
 */
@Component
@Slf4j
public class UserManagementClient {
    @Value("${userManagement.baseUrl}")
    private String baseUrl;

    private RestTemplate httpClient;
    private final ServiceTokenProvider serviceTokenProvider;

    public UserManagementClient(ServiceTokenProvider serviceTokenProvider) {
        this.httpClient = new RestTemplate();
        this.serviceTokenProvider = serviceTokenProvider;
    }

    public void setHttpClient(RestTemplate restTemplate) {
        this.httpClient = restTemplate;
    }

    /**
     * Uploads role configuration to User Management.
     *
     * @param roles Role configuration model.
     * @throws IOException if file cannot be loaded.
     * @throws ResponseStatusException if response is not successful.
     */
    public void publishRoles(ApplicationRoles roles) throws IOException {
        String rolesRequest = new ObjectMapper().writeValueAsString(roles);

        final HttpEntity<String> payload = new HttpEntity<>(rolesRequest, getHeaders());
        final String url = String.format("%s/api/v2/application/roles", baseUrl);

        ResponseEntity<?> response =
                httpClient.exchange(url, HttpMethod.PUT, payload, Object.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(response.getStatusCode());
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(serviceTokenProvider.getServiceToken());
        return headers;
    }
}
