package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.vavr.control.Either;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.ostelco.prime.client.api.model.Consent;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.db.SubscriberDAO;
import org.ostelco.topup.api.util.AccessToken;

import java.util.List;
import java.util.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Consents API tests.
 *
 */
public class ConsentsResourceTest {

    private static final SubscriberDAO DAO = mock(SubscriberDAO.class);
    private static final OAuthAuthenticator AUTHENTICATOR = mock(OAuthAuthenticator.class);

    private static final String key = "secret";

    private final String email = "mw@internet.org";

    private final List<Consent> consents = io.vavr.collection.List.of(
            new Consent("1", "blabla", false),
            new Consent("2", "blabla", true))
        .toJavaList();

    @ClassRule
    public static final ResourceTestRule RULE = ResourceTestRule.builder()
        .addResource(new AuthDynamicFeature(
                        new OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                        .setAuthenticator(AUTHENTICATOR)
                        .setPrefix("Bearer")
                        .buildAuthFilter()))
        .addResource(new AuthValueFactoryProvider.Binder<>(AccessTokenPrincipal.class))
        .addResource(new ConsentsResource(DAO))
        .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
        .build();

    @Before
    public void setUp()  throws Exception {
        when(AUTHENTICATOR.authenticate(anyString()))
            .thenReturn(Optional.of(new AccessTokenPrincipal(email)));
    }

    @Test
    public void getConsents() throws Exception {
        ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);

        when(DAO.getConsents(arg.capture())).thenReturn(Either.right(consents));

        Response resp = RULE.target("/consents")
            .request()
            .header("Authorization", String.format("Bearer %s", AccessToken.withEmail(email)))
            .get(Response.class);

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.readEntity(new GenericType<List<Consent>>() {})).isEqualTo(consents);
        assertThat(arg.getValue()).isEqualTo(email);
    }

    @Test
    public void acceptConsent() throws Exception {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);

        final String consentId = consents.get(0).getConsentId();

        when(DAO.acceptConsent(arg1.capture(), arg2.capture())).thenReturn(Either.right(consents.get(0)));
        when(DAO.rejectConsent(arg1.capture(), arg2.capture())).thenReturn(Either.left(
                        new Error("No consents found")));

        Response resp = RULE.target(String.format("/consents/%s", consentId))
            .queryParam("accepted", true)
            .request()
            .header("Authorization", String.format("Bearer %s", AccessToken.withEmail(email)))
            .put(Entity.text(""));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(arg1.getValue()).isEqualTo(email);
        assertThat(arg2.getValue()).isEqualTo(consentId);
    }

    @Test
    public void rejectConsent() throws Exception {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);

        final String consentId = consents.get(0).getConsentId();

        when(DAO.acceptConsent(arg1.capture(), arg2.capture())).thenReturn(Either.left(
                        new Error("No consents found")));
        when(DAO.rejectConsent(arg1.capture(), arg2.capture())).thenReturn(Either.right(consents.get(0)));

        Response resp = RULE.target(String.format("/consents/%s", consentId))
            .queryParam("accepted", false)
            .request()
            .header("Authorization", String.format("Bearer %s", AccessToken.withEmail(email)))
            .put(Entity.text(""));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(arg1.getValue()).isEqualTo(email);
        assertThat(arg2.getValue()).isEqualTo(consentId);
    }
}
