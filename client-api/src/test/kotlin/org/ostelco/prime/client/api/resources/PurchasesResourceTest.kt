package org.ostelco.prime.client.api.resources

import arrow.core.Either
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.nhaarman.mockito_kotlin.argumentCaptor
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter.Builder
import io.dropwizard.jackson.Jackson
import io.dropwizard.testing.junit.ResourceTestRule
import org.assertj.core.api.Assertions
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.auth.OAuthAuthenticator
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.client.api.util.AccessToken
import org.ostelco.prime.core.ApiError
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
            """|{
               |  "issuer": "someone",
               |  "email": "mw@internet.org"
               |}""".trimMargin()
                    .toByteArray())

    @Before
    @Throws(Exception::class)
    fun setUp() {
        Mockito.`when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(email)))
    }

    @Test
    fun testGetPurchaseRecords() {
        val arg1 = argumentCaptor<String>()

        val product = Product("1", Price(10, "NOK"), Collections.emptyMap(), Collections.emptyMap())
        val now = Instant.now().toEpochMilli()
        val purchaseRecord = PurchaseRecord(
                product = product,
                timestamp = now,
                id = UUID.randomUUID().toString(),
                msisdn = "")

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
                .setMapper(Jackson.newObjectMapper().registerModule(KotlinModule()))
                .addResource(AuthDynamicFeature(
                        Builder<AccessTokenPrincipal>()
                                .setAuthenticator(AUTHENTICATOR)
                                .setPrefix("Bearer")
                                .buildAuthFilter()))
                .addResource(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
                .addResource(ProductsResource(DAO))
                .addResource(PurchaseResource(DAO))
                .setTestContainerFactory(GrizzlyWebTestContainerFactory())
                .build()
    }
}