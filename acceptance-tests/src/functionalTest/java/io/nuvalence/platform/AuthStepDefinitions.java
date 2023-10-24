package io.nuvalence.platform;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.nuvalence.platform.utils.cucumber.contexts.AuthorizationContext;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class AuthStepDefinitions {
    private final AuthorizationContext authorizationContext;

    public AuthStepDefinitions(AuthorizationContext authorizationContext) {
        this.authorizationContext = authorizationContext;
    }

    @Given("a user with the following roles")
    public void createUserToken(DataTable dataTable) {
        final List<String> roles =
                dataTable.asMaps().stream()
                        .map(map -> map.get("role"))
                        .collect(Collectors.toList());
        authorizationContext.generateToken(UUID.randomUUID().toString(), roles);
    }

    @Given("a caller without a valid auth token")
    public void callerWithoutAValidAuthToken() {
        authorizationContext.clear();
    }
}
