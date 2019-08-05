package org.ostelco.prime.support

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.support.resources.AuditLogResource
import org.ostelco.prime.support.resources.BundlesResource
import org.ostelco.prime.support.resources.ContextResource
import org.ostelco.prime.support.resources.NotifyResource
import org.ostelco.prime.support.resources.ProfilesResource
import org.ostelco.prime.support.resources.PurchaseResource
import org.ostelco.prime.support.resources.RefundResource

@JsonTypeName("support")
class SupportModule : PrimeModule {

    override fun init(env: Environment) {
        val jerseySever = env.jersey()
        jerseySever.register(ProfilesResource())
        jerseySever.register(BundlesResource())
        jerseySever.register(PurchaseResource())
        jerseySever.register(RefundResource())
        jerseySever.register(NotifyResource())
        jerseySever.register(ContextResource())
        jerseySever.register(AuditLogResource())
    }
}