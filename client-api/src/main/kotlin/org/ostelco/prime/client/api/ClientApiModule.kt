package org.ostelco.prime.client.api

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.ostelco.prime.client.api.metrics.reportMetricsAtStartUp
import org.ostelco.prime.client.api.resources.*
import org.ostelco.prime.client.api.store.SubscriberDAOImpl
import org.ostelco.prime.module.PrimeModule
import java.util.*
import javax.servlet.DispatcherType


/**
 * Provides API for client.
 *
 */
@JsonTypeName("api")
class ClientApiModule : PrimeModule {

    override fun init(env: Environment) {

        // Allow CORS
        val corsFilterRegistration = env.servlets().addFilter("CORS", CrossOriginFilter::class.java)
        // Configure CORS parameters
        corsFilterRegistration.setInitParameter("allowedOrigins", "*")
        corsFilterRegistration.setInitParameter("allowedHeaders",
                "Cache-Control,If-Modified-Since,Pragma,Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin")
        corsFilterRegistration.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD")
        corsFilterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/*")


        val dao = SubscriberDAOImpl()
        val jerseyEnv = env.jersey()

        /* APIs. */
        jerseyEnv.register(AnalyticsResource(dao))
        jerseyEnv.register(ConsentsResource(dao))
        jerseyEnv.register(ProductsResource(dao))
        jerseyEnv.register(PurchaseResource(dao))
        jerseyEnv.register(ProfileResource(dao))
        jerseyEnv.register(ReferralResource(dao))
        jerseyEnv.register(PaymentSourcesResource(dao))
        jerseyEnv.register(SubscriptionResource(dao))
        jerseyEnv.register(BundlesResource(dao))
        jerseyEnv.register(SubscriptionsResource(dao))
        jerseyEnv.register(CustomerResource(dao))
        jerseyEnv.register(ApplicationTokenResource(dao))

        reportMetricsAtStartUp()
    }
}
