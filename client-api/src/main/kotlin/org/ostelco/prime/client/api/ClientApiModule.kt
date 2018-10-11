package org.ostelco.prime.client.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.CachingAuthenticator
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter.Builder
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.setup.Environment
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.auth.OAuthAuthenticator
import org.ostelco.prime.client.api.metrics.reportMetricsAtStartUp
import org.ostelco.prime.client.api.resources.AnalyticsResource
import org.ostelco.prime.client.api.resources.ApplicationTokenResource
import org.ostelco.prime.client.api.resources.BundlesResource
import org.ostelco.prime.client.api.resources.ConsentsResource
import org.ostelco.prime.client.api.resources.PaymentResource
import org.ostelco.prime.client.api.resources.ProductsResource
import org.ostelco.prime.client.api.resources.ProfileResource
import org.ostelco.prime.client.api.resources.PurchaseResource
import org.ostelco.prime.client.api.resources.ReferralResource
import org.ostelco.prime.client.api.resources.SubscriptionResource
import org.ostelco.prime.client.api.resources.SubscriptionsResource
import org.ostelco.prime.client.api.store.SubscriberDAOImpl
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.module.getResource
import org.ostelco.prime.ocs.OcsSubscriberService
import org.ostelco.prime.storage.ClientDataSource
import java.util.*
import javax.servlet.DispatcherType
import javax.ws.rs.client.Client


/**
 * Provides API for client.
 *
 */
@JsonTypeName("api")
class ClientApiModule : PrimeModule {

    @JsonProperty("config")
    private var config: ClientApiConfiguration = ClientApiConfiguration()

    private val storage by lazy { getResource<ClientDataSource>() }
    private val ocsSubscriberService by lazy { getResource<OcsSubscriberService>() }

    override fun init(env: Environment) {

        // Allow CORS
        val corsFilterRegistration = env.servlets().addFilter("CORS", CrossOriginFilter::class.java)
        // Configure CORS parameters
        corsFilterRegistration.setInitParameter("allowedOrigins", "*")
        corsFilterRegistration.setInitParameter("allowedHeaders",
                "Cache-Control,If-Modified-Since,Pragma,Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin")
        corsFilterRegistration.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD")
        corsFilterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/*")


        val dao = SubscriberDAOImpl(storage, ocsSubscriberService)
        val jerseyEnv = env.jersey()

        val client: Client = JerseyClientBuilder(env)
                .using(config.jerseyClientConfiguration)
                .using(jacksonObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
                .build(env.name)

        /* APIs. */
        jerseyEnv.register(AnalyticsResource(dao))
        jerseyEnv.register(ConsentsResource(dao))
        jerseyEnv.register(ProductsResource(dao))
        jerseyEnv.register(PurchaseResource(dao))
        jerseyEnv.register(ProfileResource(dao))
        jerseyEnv.register(ReferralResource(dao))
        jerseyEnv.register(PaymentResource(dao))
        jerseyEnv.register(SubscriptionResource(dao))
        jerseyEnv.register(BundlesResource(dao))
        jerseyEnv.register(SubscriptionsResource(dao))
        jerseyEnv.register(ApplicationTokenResource(dao))

        /* OAuth2 with cache. */
        val authenticator = CachingAuthenticator(env.metrics(),
                OAuthAuthenticator(client),
                config.authenticationCachePolicy)

        jerseyEnv.register(AuthDynamicFeature(
                Builder<AccessTokenPrincipal>()
                        .setAuthenticator(authenticator)
                        .setPrefix("Bearer")
                        .buildAuthFilter()))
        jerseyEnv.register(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))

        reportMetricsAtStartUp()
    }
}
