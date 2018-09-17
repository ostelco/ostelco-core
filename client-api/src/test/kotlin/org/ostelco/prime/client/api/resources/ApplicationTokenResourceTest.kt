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
import org.ostelco.prime.model.ApplicationToken
import java.util.*
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * ApplicationToken API tests.
 *
 */
class ApplicationTokenResourceTest {

    private val email = "boaty@internet.org"

    private val token = "testToken:kshfkajhka"
    private val applicationID = "myAppID:4378932"
    private val tokenType = "FCM"

    private val applicationToken = ApplicationToken(
            applicationID = applicationID,
            token = token,
            tokenType = tokenType)

    @Before
    @Throws(Exception::class)
    fun setUp() {
        `when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(email)))
    }

    @Test
    @Throws(Exception::class)
    fun storeApplicationToken() {
        val arg1 = argumentCaptor<String>()
        val arg2 = argumentCaptor<ApplicationToken>()

        val argMsisdn = argumentCaptor<String>()
        val msisdn = "4790300001"

        `when`(DAO.storeApplicationToken(arg1.capture(), arg2.capture()))
                .thenReturn(Either.right(applicationToken))
        `when`<Either<ApiError, String>>(DAO.getMsisdn(argMsisdn.capture())).thenReturn(Either.right(msisdn))

        val resp = RULE.target("/applicationtoken")
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .post(Entity.json("{\n" +
                        "    \"token\": \"" + token + "\",\n" +
                        "    \"applicationID\": \"" + applicationID + "\",\n" +
                        "    \"tokenType\": \"" + tokenType + "\"\n" +
                        "}\n"))

        assertThat(resp.status).isEqualTo(Response.Status.CREATED.statusCode)
        assertThat(resp.mediaType.toString()).isEqualTo(MediaType.APPLICATION_JSON)
        assertThat(arg1.firstValue).isEqualTo(msisdn)
        assertThat(arg2.firstValue.token).isEqualTo(token)
        assertThat(arg2.firstValue.applicationID).isEqualTo(applicationID)
        assertThat(arg2.firstValue.tokenType).isEqualTo(tokenType)
    }

    companion object {

        val DAO = mock(SubscriberDAO::class.java)
        val AUTHENTICATOR = mock(OAuthAuthenticator::class.java)
        val client: Client = mock(Client::class.java)

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
                .addResource(ApplicationTokenResource(DAO))
                .setTestContainerFactory(GrizzlyWebTestContainerFactory())
                .build()
    }
}
