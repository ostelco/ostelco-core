package org.ostelco.topup.api;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;

import javax.ws.rs.client.Client;

import org.ostelco.prime.module.PrimeModule;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.db.SubscriberDAO;
import org.ostelco.topup.api.db.SubscriberDAOInMemoryImpl;
import org.ostelco.topup.api.resources.AnalyticsResource;
import org.ostelco.topup.api.resources.ConsentsResource;
import org.ostelco.topup.api.resources.ProductsResource;
import org.ostelco.topup.api.resources.ProfileResource;
import org.ostelco.topup.api.resources.SubscriptionResource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides API for "top-up" client.
 *
 */
@JsonTypeName("api")
public class TopupModule implements PrimeModule {

    /* Load default configuration. */
    private TopupConfiguration config = new TopupConfiguration();

    /* Allows for overriding the default configuration. */
    @JsonProperty("config")
    public void setConfig() {
        config = new TopupConfiguration();
    }

    @Override
    public void init(final Environment env) {
        checkNotNull(env);

        // final SubscriberDAO dao = new SubscriberDAOImpl(DatastoreOptions.getDefaultInstance().getService());
        final SubscriberDAO dao = new SubscriberDAOInMemoryImpl();

        /* APIs. */
        env.jersey().register(new AnalyticsResource(dao));
        env.jersey().register(new ConsentsResource(dao));
        env.jersey().register(new ProductsResource(dao));
        env.jersey().register(new ProfileResource(dao));
        env.jersey().register(new SubscriptionResource(dao));

        /* For reporting OAuth2 caching events. */
        MetricRegistry metrics = SharedMetricRegistries.getOrCreate(env.getName());

        Client client = new JerseyClientBuilder(env)
            .using(config.getJerseyClientConfiguration())
            .using(new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
            .build(env.getName());

        /* OAuth2 with cache. */
        CachingAuthenticator authenticator = new CachingAuthenticator(metrics,
                new OAuthAuthenticator(client, config.getSecret()),
                config.getAuthenticationCachePolicy());

        env.jersey().register(new AuthDynamicFeature(
                        new OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                        .setAuthenticator(authenticator)
                        .setPrefix("Bearer")
                        .buildAuthFilter()));
        env.jersey().register(new AuthValueFactoryProvider.Binder<>(AccessTokenPrincipal.class));
    }
}
