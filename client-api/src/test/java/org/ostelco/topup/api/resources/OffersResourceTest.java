package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Offer;
import org.ostelco.topup.api.db.SubscriberDAO;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.Base64;
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
 * Offers API tests.
 *
 */
public class OffersResourceTest {

    private static final SubscriberDAO DAO = mock(SubscriberDAO.class);

    private static final String key = "secret";
    private final String subscriptionId = "007";
    private final String issuer = "http://ostelco.org/";
    private final String email = "mw@internet.org";
    private String accessToken;
    private final List<Offer> offers = io.vavr.collection.List.of(
            new Offer("1", "Great offer!", 10.00F, 5, 0L),
            new Offer("2", "Big time!", 5.00F, 5, 0L),
            new Offer("3", "Ultimate package!", 20.00F, 50, 0L))
        .toJavaList();
    private final String userInfo = Base64.getEncoder()
        .encodeToString((new String("{\n" +
                                    "     \"issuer\": \"someone\",\n" +
                                    "     \"email\": \"mw@internet.org\"\n" +
                                    "}\n"))
                .getBytes());

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
        .addResource(new OffersResource(DAO))
        .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
        .build();

    @Test
    public void getOffers() throws Exception {
        ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);

        when(DAO.getOffers(arg.capture())).thenReturn(Either.right(offers));

        Response resp = RULE.target("/offers")
            .request()
            .header("Authorization", String.format("Bearer %s", accessToken))
            .header("X-Endpoint-API-UserInfo", userInfo)
            .get(Response.class);

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.readEntity(new GenericType<List<Offer>>() {})).isEqualTo(offers);
        assertThat(arg.getValue()).isEqualTo(email);
    }

    @Test
    public void acceptOffer() throws Exception {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);

        final String offerId = offers.get(0).getOfferId();

        when(DAO.acceptOffer(arg1.capture(), arg2.capture())).thenReturn(Option.of(null));
        when(DAO.rejectOffer(arg1.capture(), arg2.capture())).thenReturn(Option.of(new Error()));

        Response resp = RULE.target(String.format("/offers/%s", offerId))
            .queryParam("accepted", true)
            .request()
            .header("Authorization", String.format("Bearer %s", accessToken))
            .header("X-Endpoint-API-UserInfo", userInfo)
            .put(Entity.text(""));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(arg1.getValue()).isEqualTo(email);
        assertThat(arg2.getValue()).isEqualTo(offerId);
    }

    @Test
    public void rejectOffer() throws Exception {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);

        final String offerId = offers.get(0).getOfferId();

        when(DAO.acceptOffer(arg1.capture(), arg2.capture())).thenReturn(Option.of(new Error()));
        when(DAO.rejectOffer(arg1.capture(), arg2.capture())).thenReturn(Option.of(null));

        Response resp = RULE.target(String.format("/offers/%s", offerId))
            .queryParam("accepted", false)
            .request()
            .header("Authorization", String.format("Bearer %s", accessToken))
            .header("X-Endpoint-API-UserInfo", userInfo)
            .put(Entity.text(""));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(arg1.getValue()).isEqualTo(email);
        assertThat(arg2.getValue()).isEqualTo(offerId);
    }
}
