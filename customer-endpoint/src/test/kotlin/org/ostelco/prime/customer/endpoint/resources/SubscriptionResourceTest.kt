package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.Before
import org.junit.ClassRule
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.auth.OAuthAuthenticator
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import java.time.Instant
import java.util.*

/**
 * Subscription API tests.
 *
 */
class SubscriptionResourceTest {

    private val email = "mw@internet.org"

    private val purchaseRecords = listOf(
            PurchaseRecord(
                    product = Product(sku = "1", price = Price(10, "NOK")),
                    timestamp = Instant.now().toEpochMilli(),
                    id = UUID.randomUUID().toString()))

    @Before
    fun setUp() {
        `when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(email, "email")))
    }

    companion object {

        val DAO: SubscriberDAO = mock(SubscriberDAO::class.java)
        val AUTHENTICATOR: OAuthAuthenticator = mock(OAuthAuthenticator::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule.builder()
                .setMapper(objectMapper)
                .addResource(AuthDynamicFeature(
                        OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                                .setAuthenticator(AUTHENTICATOR)
                                .setPrefix("Bearer")
                                .buildAuthFilter()))
                .addResource(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
                .addResource(CustomerResource(DAO))
                .build()
    }
}
