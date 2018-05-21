package org.ostelco.topup.api.resources;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vavr.collection.HashMap;
import io.vavr.control.Either;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.ostelco.prime.client.api.model.Product;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.core.SubscriptionStatus;
import org.ostelco.topup.api.db.SubscriberDAO;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SubscriptionResourceTest {

    private static final SubscriberDAO DAO = mock(SubscriberDAO.class);

    private static final String key = "secret";
    private final String issuer = "http://ostelco.org/";
    private final String email = "mw@internet.org";
    private final Map<String, Object> claims = HashMap.of(issuer + "email", (Object) email)
            .toJavaMap();
    private final String accessToken = Jwts.builder()
            .setClaims(claims)
            .setIssuer(issuer)
            .signWith(SignatureAlgorithm.HS512, key)
            .compact();
    private final List<Product> acceptedProducts = io.vavr.collection.List.of(
            new Product("1", 10.00F, "NOK"),
            new Product("2", 5.00F, "NOK"),
            new Product("3", 20.00F, "NOK"))
            .toJavaList();
    private final SubscriptionStatus subscriptionStatus = new SubscriptionStatus(5, acceptedProducts);

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

        // assertThat and assertEquals is not working
        assertTrue(subscriptionStatus.equals(resp.readEntity(SubscriptionStatus.class)));
        assertThat(arg.getValue()).isEqualTo(email);
    }
}
