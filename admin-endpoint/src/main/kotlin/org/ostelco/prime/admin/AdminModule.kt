package org.ostelco.prime.admin

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.admin.importer.ImportAdapter
import org.ostelco.prime.admin.resources.ImporterResource
import org.ostelco.prime.admin.resources.KYCResource
import org.ostelco.prime.admin.resources.OfferResource
import org.ostelco.prime.admin.resources.PaymentTransactionResource
import org.ostelco.prime.admin.resources.PlanResource
import org.ostelco.prime.admin.resources.SegmentResource
import org.ostelco.prime.admin.resources.SubscriptionsResource
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("admin")
class AdminModule : PrimeModule {

    override fun init(env: Environment) {
        val jerseyServer = env.jersey()
        jerseyServer.register(SubscriptionsResource())
        jerseyServer.register(OfferResource())
        jerseyServer.register(SegmentResource())
        jerseyServer.register(ImporterResource(ImportAdapter()))
        jerseyServer.register(PlanResource())
        jerseyServer.register(KYCResource())
        jerseyServer.register(PaymentTransactionResource())
    }
}
