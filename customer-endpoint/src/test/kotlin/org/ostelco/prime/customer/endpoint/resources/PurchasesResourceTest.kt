package org.ostelco.prime.customer.endpoint.resources

import arrow.core.Either
import com.nhaarman.mockitokotlin2.argumentCaptor
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter.Builder
import io.dropwizard.testing.junit.ResourceTestRule
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.auth.OAuthAuthenticator
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.customer.endpoint.util.AccessToken
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import java.time.Instant
import java.util.*

/**
 * Purchases API tests.
 *
 */
class PurchasesResourceTest {

    private val email = "mw@internet.org"

    private val userInfo = Base64.getEncoder().encodeToString(
            """{
                 "issuer": "someone",
                 "email": "mw@internet.org"
               }""".trimIndent()
                    .toByteArray())

    @Before
    fun setUp() {
        Mockito.`when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(Identity(email, "EMAIL","email"))))
    }

    @Test
    fun testGetPurchaseRecords() {
        val arg1 = argumentCaptor<Identity>()

        val product = Product("1", Price(10, "NOK"), Collections.emptyMap(), Collections.emptyMap())
        val now = Instant.now().toEpochMilli()
        val purchaseRecord = PurchaseRecord(
                product = product,
                timestamp = now,
                id = UUID.randomUUID().toString())

        Mockito.`when`<Either<ApiError, Collection<PurchaseRecord>>>(DAO.getPurchaseHistory(arg1.capture()))
                .thenReturn(Either.right(listOf(purchaseRecord)))

        val purchaseRecords = RULE.target("/purchases")
                .request()
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .header("X-Endpoint-API-UserInfo", userInfo)
                .get(Array<PurchaseRecord>::class.java)

        Assertions.assertThat(purchaseRecords).isEqualTo(arrayOf(purchaseRecord))
    }

    companion object {

        val DAO: SubscriberDAO = Mockito.mock(SubscriberDAO::class.java)
        val AUTHENTICATOR: OAuthAuthenticator = Mockito.mock(OAuthAuthenticator::class.java)

        @JvmField
        @ClassRule
        val RULE = ResourceTestRule.builder()
                .setMapper(objectMapper)
                .addResource(AuthDynamicFeature(
                        Builder<AccessTokenPrincipal>()
                                .setAuthenticator(AUTHENTICATOR)
                                .setPrefix("Bearer")
                                .buildAuthFilter()))
                .addResource(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
                .addResource(ProductsResource(DAO))
                .addResource(PurchaseResource(DAO))
                .build()
    }
}