package org.ostelco.prime.admin.api

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.getResource
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.storage.DataStore

@JsonTypeName("admin")
class AdminModule: PrimeModule {

    private var datastore: DataStore = getResource()

    override fun init(env: Environment) {
        val jerseySever = env.jersey()
        jerseySever.register(OfferResource())
        jerseySever.register(SegmentResource(datastore))
        jerseySever.register(ProductResource(datastore))
        jerseySever.register(ProductClassResource(datastore))
    }
}
