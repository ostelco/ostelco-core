package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.core.AcceptedOffer;
import org.ostelco.topup.api.core.SubscriptionStatus;
import org.ostelco.topup.api.db.SubscriberDAO;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vavr.control.Either;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class SubscriptionResourceTest {

    private static final SubscriberDAO DAO = mock(SubscriberDAO.class);

    private static final String key = "secret";
    private final String subscriptionId = "007";
    private final String issuer = "http://ostelco.org/";
    private final String email = "mw@internet.org";
    private String accessToken;
    private final List<AcceptedOffer> acceptedOffers = io.vavr.collection.List.of(
            new AcceptedOffer("1", 5, 1, 0L),
            new AcceptedOffer("2", 10, 7, 0L),
            new AcceptedOffer("3", 15, 0, 0L))
        .toJavaList();
    private final SubscriptionStatus subscriptionStatus = new SubscriptionStatus(5, acceptedOffers);

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
        .addResource(new SubscriptionResource(DAO))
        .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
        .build();

    @Test
    public void getSubscriptionStatus() throws Exception {
        ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);

        when(DAO.getSubscriptionStatus(arg.capture())).thenReturn(Either.right(subscriptionStatus));

        Response resp = RULE.target("/subscription/status")
            .request()
            .header("Authorization", String.format("Bearer %s", accessToken))
            .get(Response.class);

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.readEntity(SubscriptionStatus.class)).isEqualTo(subscriptionStatus);
        assertThat(arg.getValue()).isEqualTo(email);
    }
}
