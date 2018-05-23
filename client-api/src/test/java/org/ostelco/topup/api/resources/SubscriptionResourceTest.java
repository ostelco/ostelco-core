package org.ostelco.topup.api.resources;

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
import org.ostelco.prime.client.api.model.SubscriptionStatus;
import org.ostelco.prime.model.Price;
import org.ostelco.prime.model.Product;
import org.ostelco.prime.model.PurchaseRecord;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.db.SubscriberDAO;
import org.ostelco.topup.api.util.AccessToken;

import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Subscription API tests.
 *
 */
public class SubscriptionResourceTest {

    private static final SubscriberDAO DAO = mock(SubscriberDAO.class);
    private static final OAuthAuthenticator AUTHENTICATOR = mock(OAuthAuthenticator.class);

    private final String email = "mw@internet.org";

    private final List<PurchaseRecord> purchaseRecords = io.vavr.collection.List.of(
            new PurchaseRecord(
                    "msisdn",
                    new Product("1", new Price(10, "NOK"), emptyMap(), emptyMap()),
                    Instant.now().toEpochMilli()))
            .toJavaList();

    private final SubscriptionStatus subscriptionStatus = new SubscriptionStatus(5, purchaseRecords);

    @ClassRule
    public static final ResourceTestRule RULE = ResourceTestRule.builder()
            .addResource(new AuthDynamicFeature(
                    new OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                            .setAuthenticator(AUTHENTICATOR)
                            .setPrefix("Bearer")
                            .buildAuthFilter()))
            .addResource(new AuthValueFactoryProvider.Binder<>(AccessTokenPrincipal.class))
            .addResource(new SubscriptionResource(DAO))
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .build();

    @Before
    public void setUp()  throws Exception {
        when(AUTHENTICATOR.authenticate(anyString()))
            .thenReturn(Optional.of(new AccessTokenPrincipal(email)));
    }

    @Test
    public void getSubscriptionStatus() throws Exception {
        ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);

        when(DAO.getSubscriptionStatus(arg.capture())).thenReturn(Either.right(subscriptionStatus));

        Response resp = RULE.target("/subscription/status")
                .request()
                .header("Authorization", String.format("Bearer %s", AccessToken.withEmail(email)))
                .get(Response.class);

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);

        // assertThat and assertEquals is not working
        assertTrue(subscriptionStatus.equals(resp.readEntity(SubscriptionStatus.class)));
        assertThat(arg.getValue()).isEqualTo(email);
    }
}
