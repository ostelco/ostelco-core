package org.ostelco.prime.customer.endpoint

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.customer.endpoint.metrics.reportMetricsAtStartUp
import org.ostelco.prime.customer.endpoint.resources.ApplicationTokenResource
import org.ostelco.prime.customer.endpoint.resources.BundlesResource
import org.ostelco.prime.customer.endpoint.resources.ContextResource
import org.ostelco.prime.customer.endpoint.resources.CustomerResource
import org.ostelco.prime.customer.endpoint.resources.PaymentSourcesResource
import org.ostelco.prime.customer.endpoint.resources.ProductsResource
import org.ostelco.prime.customer.endpoint.resources.PurchaseResource
import org.ostelco.prime.customer.endpoint.resources.ReferralResource
import org.ostelco.prime.customer.endpoint.resources.RegionsResource
import org.ostelco.prime.customer.endpoint.store.SubscriberDAOImpl
import org.ostelco.prime.module.PrimeModule


/**
 * Provides API for client.
 *
 */
@JsonTypeName("api")
class CustomerEndpointModule : PrimeModule {

    override fun init(env: Environment) {

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

        reportMetricsAtStartUp()
    }
}
