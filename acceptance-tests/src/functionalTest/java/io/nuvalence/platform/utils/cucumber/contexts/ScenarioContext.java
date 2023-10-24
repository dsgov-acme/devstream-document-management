package io.nuvalence.platform.utils.cucumber.contexts;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import java.io.InputStream;
import java.util.Optional;

/**
 * Shared context/data to be retained throughout a test scenario.
 * This is injected by cucumber-picocontainer in any step definitions
 * class which takes this as a constructor argument.
 */
@Getter
@Setter
public class ScenarioContext {
    private static final String baseUri =
            Optional.ofNullable(System.getenv("SERVICE_URI")).orElse("http://api.dsgov.test/dm");

    private InputStream loadedResource;

    private final AuthorizationContext authorizationContext;

    public ScenarioContext(AuthorizationContext authorizationContext) {
        this.authorizationContext = authorizationContext;
    }

    public String getBaseUri() {
        return baseUri;
    }

    /**
     * Adds authorization header to request.
     * @param request http request
     * @param context http context
     */
    public void applyAuthorization(HttpRequest request, HttpContext context) {
        if (authorizationContext.getToken() != null) {
            request.setHeader("authorization", "Bearer " + authorizationContext.getToken());
        }
    }
}
