package org.ostelco.prime.jersey

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.cache.CacheBuilderSpec
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.CachingAuthenticator
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter.Builder
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.client.JerseyClientConfiguration
import io.dropwizard.setup.Environment
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.auth.OAuthAuthenticator
import org.ostelco.prime.module.PrimeModule
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.client.Client

@JsonTypeName("jersey")
class JerseyModule : PrimeModule {

    @JsonProperty
    var config: Config = Config()

    override fun init(env: Environment) {

        val jerseyEnv = env.jersey()

        // Read incoming YAML requests
        jerseyEnv.register(YamlMessageBodyReader::class.java)

        // filter to set TraceID in Logging MDC
        jerseyEnv.register(TrackRequestsLoggingFilter())

        // ping resource to check connectivity
        jerseyEnv.register(PingResource())

        val client: Client = JerseyClientBuilder(env)
                .using(config.jerseyClientConfiguration)
                .using(jacksonObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
                .build(env.name)

        /* OAuth2 with cache. */
        val authenticator = CachingAuthenticator(
                env.metrics(),
                OAuthAuthenticator(client),
                config.authenticationCachePolicy)

        jerseyEnv.register(AuthDynamicFeature(
                Builder<AccessTokenPrincipal>()
                        .setAuthenticator(authenticator)
                        .setPrefix("Bearer")
                        .buildAuthFilter()))
        jerseyEnv.register(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
    }
}

class Config {

    @Valid
    @NotNull
    @get:JsonProperty("authenticationCachePolicy")
    var authenticationCachePolicy: CacheBuilderSpec? = null
        private set

    @Valid
    @NotNull
    @get:JsonProperty("jerseyClient")
    val jerseyClientConfiguration = JerseyClientConfiguration()

    @JsonProperty("authenticationCachePolicy")
    fun setAuthenticationCachePolicy(spec: String) {
        this.authenticationCachePolicy = CacheBuilderSpec.parse(spec)
    }
}