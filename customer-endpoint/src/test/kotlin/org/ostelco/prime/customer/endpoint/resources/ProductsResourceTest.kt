package org.ostelco.prime.customer.endpoint.resources

import arrow.core.Either
import com.nhaarman.mockitokotlin2.argumentCaptor
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.testing.junit.ResourceTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.auth.OAuthAuthenticator
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.customer.endpoint.util.AccessToken
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import java.util.*
import java.util.Collections.emptyMap
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

private val PAYMENT: PaymentProcessor = mock(PaymentProcessor::class.java)
class MockPaymentProcessor : PaymentProcessor by PAYMENT

/**
 * Products API tests.
 *
 */
class ProductsResourceTest {

    private val email = "mw@internet.org"

    private val products = listOf(
            Product("1", Price(10, "NOK"), emptyMap(), emptyMap()),
            Product("2", Price(5, "NOK"), emptyMap(), emptyMap()),
            Product("3", Price(20, "NOK"), emptyMap(), emptyMap()))

    private val userInfo = Base64.getEncoder().encodeToString(
            """{
                 "issuer": "someone",
                 "email": "mw@internet.org"
               }""".trimIndent()
                    .toByteArray())

    @Before
    fun setUp() {
        `when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(Identity(email, "EMAIL","email"))))
    }

    @Test
    fun getProducts() {
        val arg = argumentCaptor<Identity>()

        `when`<Either<ApiError, Collection<Product>>>(DAO.getProducts(arg.capture())).thenReturn(Either.right(products))

        val resp = RULE.target("/products")
                .request()
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .header("X-Endpoint-API-UserInfo", userInfo)
                .get(Response::class.java)

        assertThat(resp.status).isEqualTo(Response.Status.OK.statusCode)
        assertThat(resp.mediaType.toString()).isEqualTo(MediaType.APPLICATION_JSON)

        // assertThat and assertEquals is not working
        assertTrue(products == resp.readEntity(object : GenericType<List<Product>>() {

        }))
        assertThat(arg.firstValue).isEqualTo(Identity(email, "EMAIL", "email"))
    }

    @Test
    fun purchaseProduct() {
        val identityArg = argumentCaptor<Identity>()
        val skuArg = argumentCaptor<String>()
        val sourceIdArg = argumentCaptor<String>()
        val saveSourceArg = argumentCaptor<Boolean>()

        val sku = products[0].sku
        val sourceId = "amex"

        `when`(DAO.purchaseProduct(
                identityArg.capture(),
                skuArg.capture(),
                sourceIdArg.capture(),
                saveSourceArg.capture())).thenReturn(Either.right(ProductInfo(sku)))

        val resp = RULE.target("/products/$sku/purchase")
                .queryParam("sourceId", sourceId)
                .request()
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .header("X-Endpoint-API-UserInfo", userInfo)
                .post(Entity.text(""))

        assertThat(resp.status).isEqualTo(Response.Status.CREATED.statusCode)
        assertThat(identityArg.allValues.toSet()).isEqualTo(setOf(Identity(email, "EMAIL", "email")))
        assertThat(skuArg.allValues.toSet()).isEqualTo(setOf(sku))
        assertThat(sourceIdArg.allValues.toSet()).isEqualTo(setOf(sourceId))
        assertThat(saveSourceArg.allValues.toSet()).isEqualTo(setOf(false))
    }

    companion object {

        val DAO: SubscriberDAO = mock(SubscriberDAO::class.java)
        val AUTHENTICATOR: OAuthAuthenticator = mock(OAuthAuthenticator::class.java)

        @JvmField
        @ClassRule
        val RULE = ResourceTestRule.builder()
                .setMapper(objectMapper)
                .addResource(AuthDynamicFeature(
                        OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                                .setAuthenticator(AUTHENTICATOR)
                                .setPrefix("Bearer")
                                .buildAuthFilter()))
                .addResource(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
                .addResource(ProductsResource(DAO))
                .build()
    }
}
