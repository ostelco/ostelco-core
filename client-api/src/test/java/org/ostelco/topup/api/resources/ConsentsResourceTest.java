package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.core.Consent;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.db.SubscriberDAO;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Consents API tests.
 *
 */
public class ConsentsResourceTest {

    private static final SubscriberDAO DAO = mock(SubscriberDAO.class);

    private static final String key = "secret";
    private final String subscriptionId = "007";
    private final String issuer = "http://ostelco.org/";
    private final String email = "mw@internet.org";
    private String accessToken;
    private final List<Consent> consents = io.vavr.collection.List.of(
            new Consent("1", "blabla", false),
            new Consent("2", "blabla", true))
        .toJavaList();

    @Before
    public void setUp() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(issuer + "email", email);
        accessToken = Jwts.builder()
            .setClaims(claims)
            .setIssuer(issuer)
            .setSubject(subscriptionId)
            .signWith(SignatureAlgorithm.HS512, key)
            .compact();
    }

    @ClassRule
    public static final ResourceTestRule RULE = ResourceTestRule.builder()
        .addResource(new AuthDynamicFeature(
                        new OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                        .setAuthenticator(new OAuthAuthenticator(key))
                        .setPrefix("Bearer")
                        .buildAuthFilter()))
        .addResource(new AuthValueFactoryProvider.Binder<>(AccessTokenPrincipal.class))
        .addResource(new ConsentsResource(DAO))
        .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
        .build();

    @Test
    public void getConsents() throws Exception {
        ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);

        when(DAO.getConsents(arg.capture())).thenReturn(Either.right(consents));

        Response resp = RULE.target("/consents")
            .request()
            .header("Authorization", String.format("Bearer %s", accessToken))
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

        when(DAO.acceptConsent(arg1.capture(), arg2.capture())).thenReturn(Option.of(null));
        when(DAO.rejectConsent(arg1.capture(), arg2.capture())).thenReturn(Option.of(new Error()));

        Response resp = RULE.target(String.format("/consents/%s", consentId))
            .queryParam("accepted", true)
            .request()
            .header("Authorization", String.format("Bearer %s", accessToken))
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

        when(DAO.acceptConsent(arg1.capture(), arg2.capture())).thenReturn(Option.of(new Error()));
        when(DAO.rejectConsent(arg1.capture(), arg2.capture())).thenReturn(Option.of(null));

        Response resp = RULE.target(String.format("/consents/%s", consentId))
            .queryParam("accepted", false)
            .request()
            .header("Authorization", String.format("Bearer %s", accessToken))
            .put(Entity.text(""));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(arg1.getValue()).isEqualTo(email);
        assertThat(arg2.getValue()).isEqualTo(consentId);
    }
}
