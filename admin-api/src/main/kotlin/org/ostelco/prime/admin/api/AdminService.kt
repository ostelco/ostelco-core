package org.ostelco.prime.admin.api

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.getResource
import org.ostelco.prime.provider.Service
import org.ostelco.prime.storage.DataStore

@JsonTypeName("admin")
class AdminService: Service {

    private var datastore: DataStore = getResource()

    override fun init(env: Environment) {
        val jerseySever = env.jersey()
        jerseySever.register(OfferResource(datastore))
        jerseySever.register(SegmentResource(datastore))
        jerseySever.register(ProductResource(datastore))
        jerseySever.register(ProductClassResource(datastore))
    }
}
