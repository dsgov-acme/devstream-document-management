package io.nuvalence.ds4g.documentmanagement.service.usermanagement;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.ds4g.documentmanagement.service.usermanagement.models.ApplicationRoles;
import io.nuvalence.ds4g.documentmanagement.service.util.ServiceTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

class UserManagementClientTest {

    @Mock private RestTemplate restTemplate;

    @Mock private ServiceTokenProvider serviceTokenProvider;

    private UserManagementClient userManagementClient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        userManagementClient = new UserManagementClient(serviceTokenProvider);
        userManagementClient.setHttpClient(restTemplate);
    }

    @Test
    void testPublishRoles_Successful() throws IOException {
        ApplicationRoles roles = ApplicationRoles.builder().build();
        when(serviceTokenProvider.getServiceToken()).thenReturn("mockedToken");
        when(restTemplate.exchange(
                        anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        userManagementClient.publishRoles(roles);

        verify(restTemplate, times(1))
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Object.class));
    }

    @Test
    void testPublishRoles_Failure() {
        ApplicationRoles roles = ApplicationRoles.builder().build();
        when(serviceTokenProvider.getServiceToken()).thenReturn("mockedToken");
        when(restTemplate.exchange(
                        anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> userManagementClient.publishRoles(roles));

        verify(restTemplate, times(1))
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Object.class));

        // Verify that the exception message contains the HTTP status code
        assert (exception.getMessage().contains(HttpStatus.INTERNAL_SERVER_ERROR.toString()));
    }
}
