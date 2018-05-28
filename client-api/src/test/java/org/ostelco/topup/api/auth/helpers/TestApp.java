package org.ostelco.topup.api.auth.helpers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.vavr.control.Either;
import org.mockito.ArgumentCaptor;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.db.SubscriberDAO;
import org.ostelco.topup.api.resources.ProfileResource;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import javax.ws.rs.client.Client;

public class TestApp extends Application<TestConfig> {

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public void initialize(Bootstrap<TestConfig> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor()));
    }

    @Override
    public void run(final TestConfig config, final Environment env)
        throws IOException {

        final SubscriberDAO DAO = mock(SubscriberDAO.class);

        ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);
        when(DAO.getProfile(arg.capture()))
            .thenReturn(Either.left(new Error("No profile found")));

        /* APIs. */
        env.jersey().register(new ProfileResource(DAO));
        env.jersey().register(new UserInfoResource());

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

        /* OAuth2. */
        env.jersey().register(new AuthDynamicFeature(
                        new OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                        .setAuthenticator(authenticator)
                        .setPrefix("Bearer")
                        .buildAuthFilter()));
        env.jersey().register(new AuthValueFactoryProvider.Binder<>(AccessTokenPrincipal.class));
    }
}
