package org.ostelco.topup.api.resources;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.vavr.control.Option;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.db.SubscriberDAO;
import org.ostelco.topup.api.util.AccessToken;

import java.io.IOException;
import java.util.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Analytics API tests.
 *
 */
public class AnalyticsResourceTest {

    private final ObjectMapper MAPPER = new ObjectMapper();

    private static final SubscriberDAO DAO = mock(SubscriberDAO.class);
    private static final OAuthAuthenticator AUTHENTICATOR = mock(OAuthAuthenticator.class);

    private final String email = "mw@internet.org";

    @ClassRule
    public static final ResourceTestRule RULE = ResourceTestRule.builder()
        .addResource(new AuthDynamicFeature(
                        new OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                        .setAuthenticator(AUTHENTICATOR)
                        .setPrefix("Bearer")
                        .buildAuthFilter()))
        .addResource(new AuthValueFactoryProvider.Binder<>(AccessTokenPrincipal.class))
        .addResource(new AnalyticsResource(DAO))
        .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
        .build();

    @Before
    public void setUp()  throws Exception {
        when(AUTHENTICATOR.authenticate(anyString()))
            .thenReturn(Optional.of(new AccessTokenPrincipal(email)));
    }

    @Test
    public void reportAnalytics() throws Exception {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);

        when(DAO.reportAnalytics(arg1.capture(), arg2.capture()))
            .thenReturn(Option.none());

        final String events = "[{\n" +
                              "    \"eventType\": \"PURCHASES_A_PRODUCT\",\n" +
                              "    \"sku\": \"1\",\n" +
                              "    \"time\": \"1524734549\"\n" +
                              "},{\n" +
                              "    \"eventType\": \"EXITS_APPLICATION\",\n" +
                              "    \"time\": \"1524742549\"\n" +
                              "}]\n";

        assertThat(isValidJson(events)).isTrue();

        Response resp = RULE.target("/analytics")
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", String.format("Bearer %s", AccessToken.withEmail(email)))
            .post(Entity.json(events));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(resp.getMediaType()).isNull();
        assertThat(arg1.getValue()).isEqualTo(email);
        assertThat(isValidJson(events)).isTrue();
        assertThat(isValidJson(arg2.getValue())).isTrue();
    }

    /* https://stackoverflow.com/questions/10226897/how-to-validate-json-with-jackson-json */
    private boolean isValidJson(final String json) {
        try {
            final JsonParser parser = MAPPER.getFactory()
                .createParser(json);
            while (parser.nextToken() != null) {
            }
            return true;
        } catch (JsonParseException e) {
            /* Ignored. */
        } catch (IOException e) {
            /* Ignored. */
        }
        return false;
    }
}
