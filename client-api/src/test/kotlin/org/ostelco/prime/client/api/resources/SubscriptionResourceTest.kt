package org.ostelco.prime.client.api.resources

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.argumentCaptor
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
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
import org.ostelco.prime.client.api.model.SubscriptionStatus
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.client.api.util.AccessToken
import org.ostelco.prime.core.ApiError
import org.ostelco.prime.model.ActivePseudonyms
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PseudonymEntity
import org.ostelco.prime.model.PurchaseRecord
import java.time.Instant
import java.util.*
import javax.ws.rs.client.Client
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Subscription API tests.
 *
 */
class SubscriptionResourceTest {

    private val email = "mw@internet.org"

    private val purchaseRecords = listOf(
            PurchaseRecord(
                    msisdn = "msisdn",
                    product = Product(sku = "1", price = Price(10, "NOK")),
                    timestamp = Instant.now().toEpochMilli()))

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

    @Test
    @Throws(Exception::class)
    fun getActivePseudonyms() {
        val arg = argumentCaptor<String>()
        val msisdn = "4790300001"
        val url = "${PSEUDONYMENDPOINT}/pseudonym/active/$msisdn"
        val pseudonym = PseudonymEntity(msisdn, "random", 0, 1)
        val activePseudonyms = ActivePseudonyms(pseudonym, pseudonym)
        val responseJsonString = ObjectMapper().writeValueAsString(activePseudonyms)
        val response = Response.status(Response.Status.OK)
                .entity(responseJsonString)
                .build()

        `when`<Either<ApiError, String>>(DAO.getMsisdn(arg.capture())).thenReturn(Either.right(msisdn))
        `when`<WebTarget>(client.target(url)).thenReturn(target)
        `when`<Invocation.Builder>(target.request()).thenReturn(request)
        `when`<Response>(request.get()).thenReturn(response)

        val resp = RULE.target("/subscription/activePseudonyms")
                .request()
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .get(Response::class.java)

        assertThat(resp.status).isEqualTo(Response.Status.OK.statusCode)
        assertThat(resp.mediaType.toString()).isEqualTo(MediaType.APPLICATION_JSON)
        assertTrue(resp.hasEntity())
        assertTrue(responseJsonString == resp.readEntity(String::class.java))
    }

    companion object {

        val DAO: SubscriberDAO = mock(SubscriberDAO::class.java)
        val AUTHENTICATOR: OAuthAuthenticator = mock(OAuthAuthenticator::class.java)
        val PSEUDONYMENDPOINT = "http://localhost"
        val client: Client = mock(Client::class.java)
        val target: WebTarget = mock(WebTarget::class.java)
        val request: Invocation.Builder = mock(Invocation.Builder::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule.builder()
                .addResource(AuthDynamicFeature(
                        OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                                .setAuthenticator(AUTHENTICATOR)
                                .setPrefix("Bearer")
                                .buildAuthFilter()))
                .addResource(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
                .addResource(SubscriptionResource(DAO, client, PSEUDONYMENDPOINT))
                .setTestContainerFactory(GrizzlyWebTestContainerFactory())
                .build()
    }
}
