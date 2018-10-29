package org.ostelco.prime.jersey.auth.helpers

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
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
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.auth.OAuthAuthenticator
import org.ostelco.prime.jsonmapper.asJson
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class TestApp : Application<TestConfig>() {

    override fun getName(): String {
        return "test"
    }

    override fun initialize(bootstrap: Bootstrap<TestConfig>) {
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
                bootstrap.configurationSourceProvider,
                EnvironmentVariableSubstitutor())
    }

    override fun run(config: TestConfig, env: Environment) {

        /* APIs. */
        env.jersey().register(UserInfoResource())
        env.jersey().register(FooResource())

        val client = JerseyClientBuilder(env)
                .using(config.jerseyClientConfiguration)
                .using(ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
                .build(env.name)

        /* OAuth2 with cache. */
        val authenticator = CachingAuthenticator(env.metrics(),
                OAuthAuthenticator(client),
                config.authenticationCachePolicy)

        /* OAuth2. */
        env.jersey().register(AuthDynamicFeature(
                OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                        .setAuthenticator(authenticator)
                        .setPrefix("Bearer")
                        .buildAuthFilter()))
        env.jersey().register(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
    }
}

@Path("/foo")
class FooResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun query(): Response = Response
            .status(Response.Status.NOT_FOUND)
            .entity(asJson(""))
            .build()
}