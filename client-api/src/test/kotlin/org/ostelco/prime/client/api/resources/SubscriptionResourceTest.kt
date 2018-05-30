package org.ostelco.prime.client.api.resources

import com.nhaarman.mockito_kotlin.argumentCaptor
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.testing.junit.ResourceTestRule
import io.vavr.control.Either
import org.assertj.core.api.Assertions.assertThat
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.auth.OAuthAuthenticator
import org.ostelco.prime.client.api.core.ApiError
import org.ostelco.prime.client.api.model.SubscriptionStatus
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.client.api.util.AccessToken
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import java.time.Instant
import java.util.*
import java.util.Collections.emptyMap
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Subscription API tests.
 *
 */
class SubscriptionResourceTest {

    private val email = "mw@internet.org"

    private val purchaseRecords = io.vavr.collection.List.of(
            PurchaseRecord(
                    "msisdn",
                    Product("1", Price(10, "NOK"), emptyMap(), emptyMap()),
                    Instant.now().toEpochMilli()))
            .toJavaList()

    private val subscriptionStatus = SubscriptionStatus(5, purchaseRecords)

    @Before
    @Throws(Exception::class)
    fun setUp() {
        `when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(email)))
    }

    @Test
    @Throws(Exception::class)
    fun getSubscriptionStatus() {
        val arg = argumentCaptor<String>()

        `when`<Either<ApiError, SubscriptionStatus>>(DAO.getSubscriptionStatus(arg.capture())).thenReturn(Either.right(subscriptionStatus))

        val resp = RULE.target("/subscription/status")
                .request()
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .get(Response::class.java)

        assertThat(resp.status).isEqualTo(Response.Status.OK.statusCode)
        assertThat(resp.mediaType.toString()).isEqualTo(MediaType.APPLICATION_JSON)

        // assertThat and assertEquals is not working
        assertTrue(subscriptionStatus == resp.readEntity(SubscriptionStatus::class.java))
        assertThat(arg.firstValue).isEqualTo(email)
    }

    companion object {

        val DAO: SubscriberDAO = mock(SubscriberDAO::class.java)
        val AUTHENTICATOR: OAuthAuthenticator = mock(OAuthAuthenticator::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule.builder()
                .addResource(AuthDynamicFeature(
                        OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                                .setAuthenticator(AUTHENTICATOR)
                                .setPrefix("Bearer")
                                .buildAuthFilter()))
                .addResource(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
                .addResource(SubscriptionResource(DAO))
                .setTestContainerFactory(GrizzlyWebTestContainerFactory())
                .build()
    }
}
