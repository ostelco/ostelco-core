package org.ostelco.prime.customer.endpoint.resources

import arrow.core.Either
import com.nhaarman.mockitokotlin2.argumentCaptor
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.testing.junit.ResourceTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.auth.OAuthAuthenticator
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.customer.endpoint.util.AccessToken
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import java.util.*
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Profile API tests.
 *
 */
class CustomerResourceTest {

    private val email = "boaty@internet.org"
    private val name = "Boaty McBoatface"

    private val profile = Customer(nickname = name, contactEmail = email)

    @Before
    fun setUp() {
        `when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(Identity(email, "EMAIL","email"))))
    }

    @Test
    fun getProfile() {
        val arg = argumentCaptor<Identity>()

        `when`(DAO.getCustomer(arg.capture())).thenReturn(Either.right(profile))

        val resp = RULE.target("/customer")
                .request()
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .get(Response::class.java)

        assertThat(resp.status).isEqualTo(Response.Status.OK.statusCode)
        assertThat(resp.mediaType.toString()).isEqualTo(MediaType.APPLICATION_JSON)

        assertThat(resp.readEntity(Customer::class.java)).isEqualTo(profile)
        assertThat(arg.firstValue).isEqualTo(Identity(email, "EMAIL", "email"))
    }

    @Test
    fun createProfile() {
        val arg1 = argumentCaptor<Identity>()
        val arg2 = argumentCaptor<Customer>()
        val arg3 = argumentCaptor<String>()


        `when`(DAO.createCustomer(arg1.capture(), arg2.capture(), arg3.capture()))
                .thenReturn(Either.right(profile))

        val resp = RULE.target("/customer")
                .queryParam("nickname", name)
                .queryParam("contactEmail", email)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .post(Entity.json("{}"))

        assertThat(resp.status).isEqualTo(Response.Status.CREATED.statusCode)
        assertThat(resp.mediaType.toString()).isEqualTo(MediaType.APPLICATION_JSON)
        assertThat(arg1.firstValue).isEqualTo(Identity(email, "EMAIL", "email"))
        assertThat(arg2.firstValue.contactEmail).isEqualTo(email)
        assertThat(arg2.firstValue.nickname).isEqualTo(name)
        assertThat(arg3.firstValue).isNull()
    }

    @Test
    fun createProfileWithReferral() {
        val arg1 = argumentCaptor<Identity>()
        val arg2 = argumentCaptor<Customer>()
        val arg3 = argumentCaptor<String>()

        val referredBy = "foo@bar.com"

        `when`(DAO.createCustomer(arg1.capture(), arg2.capture(), arg3.capture()))
                .thenReturn(Either.right(profile))

        val resp = RULE.target("/customer")
                .queryParam("nickname", name)
                .queryParam("contactEmail", email)
                .queryParam("referredBy", referredBy)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .post(Entity.json(""))

        assertThat(resp.status).isEqualTo(Response.Status.CREATED.statusCode)
        assertThat(resp.mediaType.toString()).isEqualTo(MediaType.APPLICATION_JSON)
        assertThat(arg1.firstValue).isEqualTo(Identity(email, "EMAIL", "email"))
        assertThat(arg2.firstValue.contactEmail).isEqualTo(email)
        assertThat(arg2.firstValue.nickname).isEqualTo(name)
        assertThat(arg3.firstValue).isEqualTo(referredBy)
    }

    @Test
    fun updateProfile() {
        val identityCaptor = argumentCaptor<Identity>()
        val nicknameCaptor = argumentCaptor<String>()
        val contactEmailCaptor = argumentCaptor<String>()

        `when`(DAO.updateCustomer(identityCaptor.capture(), nicknameCaptor.capture(), contactEmailCaptor.capture()))
                .thenReturn(Either.right(profile))

        val resp = RULE.target("/customer")
                .queryParam("nickname", name)
                .queryParam("contactEmail", email)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .put(Entity.json("{}"))

        assertThat(resp.status).isEqualTo(Response.Status.OK.statusCode)
        assertThat(resp.mediaType.toString()).isEqualTo(MediaType.APPLICATION_JSON)
        assertThat(identityCaptor.firstValue).isEqualTo(Identity(email, "EMAIL", "email"))
        assertThat(nicknameCaptor.firstValue).isEqualTo(name)
        assertThat(contactEmailCaptor.firstValue).isEqualTo(email)
    }

    companion object {

        val DAO = mock(SubscriberDAO::class.java)
        val AUTHENTICATOR = mock(OAuthAuthenticator::class.java)

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
                .addResource(CustomerResource(DAO))
                .build()
    }
}
