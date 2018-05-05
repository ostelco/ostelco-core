package org.ostelco.topup.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter.Builder
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.legacy.Storage
import org.ostelco.topup.api.auth.AccessTokenPrincipal
import org.ostelco.topup.api.auth.OAuthAuthenticator
import org.ostelco.topup.api.db.SubscriberDAOImpl
import org.ostelco.topup.api.resources.AnalyticsResource
import org.ostelco.topup.api.resources.ConsentsResource
import org.ostelco.topup.api.resources.ProductsResource
import org.ostelco.topup.api.resources.ProfileResource
import org.ostelco.topup.api.resources.SubscriptionResource

/**
 * Provides API for "top-up" client.
 *
 */
@JsonTypeName("api")
class TopupModule : PrimeModule {

    private var namespace: String? = null

    private val storage by lazy { getResource<Storage>() }

    @JsonProperty("namespace")
    fun setNamespace(namespace: String) {
        this.namespace = namespace
    }

    override fun init(env: Environment) {

        checkNotNull(env)
        val dao = SubscriberDAOImpl(storage)
        val jerseyEnv = env.jersey()

        /* APIs. */
        jerseyEnv.register(AnalyticsResource(dao))
        jerseyEnv.register(ConsentsResource(dao))
        jerseyEnv.register(ProductsResource(dao))
        jerseyEnv.register(ProfileResource(dao))
        jerseyEnv.register(SubscriptionResource(dao))

        /* OAuth2. */
        jerseyEnv.register(AuthDynamicFeature(
                Builder<AccessTokenPrincipal>()
                        .setAuthenticator(OAuthAuthenticator(namespace, "jwtsecret"))
                        .setPrefix("Bearer")
                        .buildAuthFilter()))
        jerseyEnv.register(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
    }
}
