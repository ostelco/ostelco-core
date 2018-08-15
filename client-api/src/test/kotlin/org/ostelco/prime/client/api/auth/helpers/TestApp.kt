package org.ostelco.prime.client.api.auth.helpers

import arrow.core.Either
import com.codahale.metrics.SharedMetricRegistries
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.argumentCaptor
import io.dropwizard.Application
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.CachingAuthenticator
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.auth.OAuthAuthenticator
import org.ostelco.prime.client.api.resources.ProfileResource
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.core.NotFoundError
import java.io.IOException

class TestApp : Application<TestConfig>() {

    override fun getName(): String {
        return "test"
    }

    override fun initialize(bootstrap: Bootstrap<TestConfig>?) {
        bootstrap!!.configurationSourceProvider = SubstitutingSourceProvider(
                bootstrap.configurationSourceProvider,
                EnvironmentVariableSubstitutor())
    }

    @Throws(IOException::class)
    override fun run(config: TestConfig, env: Environment) {

        val DAO = mock(SubscriberDAO::class.java)

        val arg = argumentCaptor<String>()
        `when`(DAO.getProfile(arg.capture()))
                .thenReturn(Either.left(NotFoundError("No profile found")))

        /* APIs. */
        env.jersey().register(ProfileResource(DAO))
        env.jersey().register(UserInfoResource())

        /* For reporting OAuth2 caching events. */
        val metrics = SharedMetricRegistries.getOrCreate(env.name)

        val client = JerseyClientBuilder(env)
                .using(config.jerseyClientConfiguration)
                .using(ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
                .build(env.name)

        /* OAuth2 with cache. */
        val authenticator = CachingAuthenticator(metrics,
                OAuthAuthenticator(client),
                config.authenticationCachePolicy!!)

        /* OAuth2. */
        env.jersey().register(AuthDynamicFeature(
                OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                        .setAuthenticator(authenticator)
                        .setPrefix("Bearer")
                        .buildAuthFilter()))
        env.jersey().register(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
    }
}
