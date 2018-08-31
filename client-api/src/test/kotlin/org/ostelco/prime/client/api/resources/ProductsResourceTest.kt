package org.ostelco.prime.client.api.resources

import arrow.core.Either
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.nhaarman.mockito_kotlin.argumentCaptor
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.jackson.Jackson
import io.dropwizard.testing.junit.ResourceTestRule
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
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.client.api.util.AccessToken
import org.ostelco.prime.core.ApiError
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
            """|{
               |  "issuer": "someone",
               |  "email": "mw@internet.org"
               |}""".trimMargin()
                    .toByteArray())

    @Before
    @Throws(Exception::class)
    fun setUp() {
        `when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(email)))
    }

    @Test
    @Throws(Exception::class)
    fun getProducts() {
        val arg = argumentCaptor<String>()

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
        assertThat(arg.firstValue).isEqualTo(email)
    }

    @Test
    @Throws(Exception::class)
    fun purchaseProduct() {
        val emailArg = argumentCaptor<String>()
        val skuArg = argumentCaptor<String>()
        val sourceIdArg = argumentCaptor<String>()
        val saveSourceArg = argumentCaptor<Boolean>()

        val sku = products[0].sku
        val sourceId = "amex"

        `when`(DAO.purchaseProduct(
                emailArg.capture(),
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
        assertThat(emailArg.allValues.toSet()).isEqualTo(setOf(email))
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
                .setMapper(Jackson.newObjectMapper().registerModule(KotlinModule()))
                .addResource(AuthDynamicFeature(
                        OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                                .setAuthenticator(AUTHENTICATOR)
                                .setPrefix("Bearer")
                                .buildAuthFilter()))
                .addResource(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
                .addResource(ProductsResource(DAO))
                .setTestContainerFactory(GrizzlyWebTestContainerFactory())
                .build()
    }
}
