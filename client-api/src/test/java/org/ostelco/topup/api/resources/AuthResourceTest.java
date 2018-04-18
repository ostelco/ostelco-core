package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.core.Grant;
import org.ostelco.topup.api.db.SubscriberDAO;

import io.dropwizard.testing.junit.ResourceTestRule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vavr.control.Either;
import java.util.UUID;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests 'auth' API.
 *
 */
public class AuthResourceTest {

    private static final SubscriberDAO DAO = mock(SubscriberDAO.class);

    private final String code = "0123456789";
    private final String key = "secret";
    private final String subscriptionId = "007";
    private final String accessToken = Jwts.builder()
        .setSubject(subscriptionId)
        .signWith(SignatureAlgorithm.HS512, key)
        .compact();
    private final String refreshToken = UUID.randomUUID()
        .toString()
        .replaceAll("-", "");
    private final String oauthToken = "{\n" +
                                      "     \"token_type\": \"Bearer\",\n" +
                                      "     \"access_token\": \"" + accessToken + "\",\n" +
                                      "     \"refresh_token\": \"" + refreshToken + "\",\n" +
                                      "     \"exp\": 3600\n" +
                                      "}\n";

    @ClassRule
    public static final ResourceTestRule RULE = ResourceTestRule.builder()
        .addResource(new AuthResource(DAO))
        .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
        .build();

    @Test
    public void verify() throws Exception {
        ArgumentCaptor<Grant> arg = ArgumentCaptor.forClass(Grant.class);

        when(DAO.handleGrant(arg.capture())).thenReturn(Either.right(oauthToken));

        final String grantType = "authorization_code";

        Response resp = RULE.target("/auth/token")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json("{\n" +
                              "    \"grantType\": \"" + grantType + "\",\n" +
                              "    \"code\": \"" + code + "\"\n" +
                              "}\n"));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.getHeaderString("Cache-Control")).isEqualTo("no-store");
        assertThat(resp.getHeaderString("Pragma")).isEqualTo("no-cache");
        assertThat(resp.readEntity(String.class)).isEqualTo(oauthToken);
        assertThat(arg.getValue().getGrantType()).isEqualTo(grantType);
        assertThat(arg.getValue().getCode()).isEqualTo(code);
    }

    @Test
    public void refresh() throws Exception {
        ArgumentCaptor<Grant> arg = ArgumentCaptor.forClass(Grant.class);

        when(DAO.handleGrant(arg.capture())).thenReturn(Either.right(oauthToken));

        final String grantType = "refresh_token";

        Response resp = RULE.target("/auth/token")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json("{\n" +
                              "    \"grantType\": \"" + grantType + "\",\n" +
                              "    \"refreshToken\": \"" + refreshToken + "\"\n" +
                              "}\n"));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.getHeaderString("Cache-Control")).isEqualTo("no-store");
        assertThat(resp.getHeaderString("Pragma")).isEqualTo("no-cache");
        assertThat(resp.readEntity(String.class)).isEqualTo(oauthToken);
        assertThat(arg.getValue().getGrantType()).isEqualTo(grantType);
        assertThat(arg.getValue().getRefreshToken()).isEqualTo(refreshToken);
    }
}
