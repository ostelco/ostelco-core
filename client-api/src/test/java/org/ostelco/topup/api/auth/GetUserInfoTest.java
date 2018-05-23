package org.ostelco.topup.api.auth;

import org.ostelco.topup.api.auth.helpers.TestApp;
import org.ostelco.topup.api.auth.helpers.TestConfig;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.vavr.collection.Array;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.ostelco.topup.api.util.AccessToken;

import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests OAuth2 callback to '.../userinfo" endpoint.
 *
 */
public class GetUserInfoTest {

    private final String email = "boaty@internet.org";
    private static final String key = "secret";

    private final List<String> audience = Array.of("http://kmmtest",
            String.format("http://localhost:%d/userinfo", RULE.getLocalPort()))
        .toJavaList();

    private Client client;

    @ClassRule
    public static final DropwizardAppRule<TestConfig> RULE =
        new DropwizardAppRule<TestConfig>(TestApp.class, ResourceHelpers.resourceFilePath("test.yaml"),
                ConfigOverride.config("secret", key));

    @Before
    public void setUp() {
        client = new JerseyClientBuilder(RULE.getEnvironment()).build("test client");
    }

    @Test
    public void getProfileNotFound() throws Exception {

        Response response = client.target(
                String.format("http://localhost:%d/profile", RULE.getLocalPort()))
            .request()
            .header("Authorization", String.format("Bearer %s", AccessToken.withEmail(email, audience)))
            .get(Response.class);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getMediaType().toString()).startsWith(MediaType.TEXT_HTML);
    }
}
