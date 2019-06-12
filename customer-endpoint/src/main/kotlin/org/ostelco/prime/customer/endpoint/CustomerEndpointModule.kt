package org.ostelco.prime.customer.endpoint

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.ostelco.prime.customer.endpoint.metrics.reportMetricsAtStartUp
import org.ostelco.prime.customer.endpoint.resources.ApplicationTokenResource
import org.ostelco.prime.customer.endpoint.resources.BundlesResource
import org.ostelco.prime.customer.endpoint.resources.ContextResource
import org.ostelco.prime.customer.endpoint.resources.CustomerResource
import org.ostelco.prime.customer.endpoint.resources.PaymentSourcesResource
import org.ostelco.prime.customer.endpoint.resources.ProductsResource
import org.ostelco.prime.customer.endpoint.resources.PurchaseResource
import org.ostelco.prime.customer.endpoint.resources.RandomUUID
import org.ostelco.prime.customer.endpoint.resources.ReferralResource
import org.ostelco.prime.customer.endpoint.resources.RegionsResource
import org.ostelco.prime.customer.endpoint.store.SubscriberDAOImpl
import org.ostelco.prime.module.PrimeModule
import java.util.*
import javax.servlet.DispatcherType


/**
 * Provides API for client.
 *
 */
@JsonTypeName("api")
class CustomerEndpointModule : PrimeModule {

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
        jerseyEnv.register(ProductsResource(dao))
        jerseyEnv.register(PurchaseResource(dao))
        jerseyEnv.register(ReferralResource(dao))
        jerseyEnv.register(PaymentSourcesResource(dao))
        jerseyEnv.register(BundlesResource(dao))
        jerseyEnv.register(RegionsResource(dao))
        jerseyEnv.register(CustomerResource(dao))
        jerseyEnv.register(ContextResource(dao))
        jerseyEnv.register(ApplicationTokenResource(dao))
        jerseyEnv.register(RandomUUID())

        reportMetricsAtStartUp()
    }
}
