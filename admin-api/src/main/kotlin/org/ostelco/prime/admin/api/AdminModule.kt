package org.ostelco.prime.admin.api

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("admin")
class AdminModule: PrimeModule {

    override fun init(env: Environment) {
        val jerseySever = env.jersey()
        jerseySever.register(SubscriptionsResource())
        jerseySever.register(OfferResource())
        jerseySever.register(SegmentResource())
        jerseySever.register(ProductResource())
        jerseySever.register(ProductClassResource())
    }
}
