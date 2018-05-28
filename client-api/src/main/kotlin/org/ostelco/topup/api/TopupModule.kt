package org.ostelco.topup.api

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SharedMetricRegistries
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.CachingAuthenticator
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter.Builder
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.module.getResource
import org.ostelco.prime.ocs.OcsSubscriberService
import org.ostelco.prime.storage.legacy.Storage
import org.ostelco.topup.api.auth.AccessTokenPrincipal
import org.ostelco.topup.api.auth.OAuthAuthenticator
import org.ostelco.topup.api.db.SubscriberDAOImpl
import org.ostelco.topup.api.resources.AnalyticsResource
import org.ostelco.topup.api.resources.ConsentsResource
import org.ostelco.topup.api.resources.ProductsResource
import org.ostelco.topup.api.resources.ProfileResource
import org.ostelco.topup.api.resources.SubscriptionResource

import javax.ws.rs.client.Client

/**
 * Provides API for "top-up" client.
 *
 */
@JsonTypeName("api")
class TopupModule : PrimeModule {

    /* Load default configuration. */
    private var config: TopupConfiguration = TopupConfiguration() 

    private val storage by lazy { getResource<Storage>() }
    private val ocsSubscriberService by lazy { getResource<OcsSubscriberService>() }

    /** Allows for overriding the default configuration. */
    @JsonProperty("config")
    fun setConfig() {
        config = TopupConfiguration()
    }

    override fun init(env: Environment) {

        checkNotNull(env)
        val dao = SubscriberDAOImpl(storage, ocsSubscriberService)
        val jerseyEnv = env.jersey()

        /* APIs. */
        jerseyEnv.register(AnalyticsResource(dao))
        jerseyEnv.register(ConsentsResource(dao))
        jerseyEnv.register(ProductsResource(dao))
        jerseyEnv.register(ProfileResource(dao))
        jerseyEnv.register(SubscriptionResource(dao))

        /* For reporting OAuth2 caching events. */
        val metrics = SharedMetricRegistries.getOrCreate(env.getName())

        val client: Client = JerseyClientBuilder(env)
            .using(config.getJerseyClientConfiguration())
            .using(ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
            .build(env.getName())

        /* OAuth2 with cache. */
        var authenticator = CachingAuthenticator(metrics,
                OAuthAuthenticator(client, config.getSecret()),
                config.getAuthenticationCachePolicy())

        jerseyEnv.register(AuthDynamicFeature(
                Builder<AccessTokenPrincipal>()
                        .setAuthenticator(authenticator)
                        .setPrefix("Bearer")
                        .buildAuthFilter()))
        jerseyEnv.register(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
    }
}
