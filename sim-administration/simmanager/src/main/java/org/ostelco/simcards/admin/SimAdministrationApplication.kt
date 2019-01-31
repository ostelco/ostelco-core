package org.ostelco.simcards.admin

import io.dropwizard.Application
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.client.JerseyClientBuilder
import org.ostelco.dropwizardutils.OpenapiResourceAdder.Companion.addOpenapiResourceToJerseyEnv
import org.ostelco.sim.es2plus.ES2PlusIncomingHeadersFilter.Companion.addEs2PlusDefaultFiltersAndInterceptors
import org.ostelco.sim.es2plus.SmDpPlusCallbackResource
import org.ostelco.sim.es2plus.SmDpPlusCallbackService
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SimInventoryResource

/**
 * The SIM manager
 * is an application that inputs inhales SIM batches
 * from SIM profile factories (physical or esim). It then facilitates
 * activation of SIM profiles to MSISDNs.   A typical interaction is
 * "find me a sim profile for this MSISDN for this HLR" , and then
 * "activate that profile".   The activation will typically involve
 * at least talking to a HLR to permit user equipment to use the
 * SIM profile to authenticate, and possibly also an SM-DP+ to
 * activate a SIM profile (via its ICCID and possible an EID).
 * The inventory can then serve as an intermidiary between the
 * rest of the BSS and the OSS in the form of HSS and SM-DP+.
 */
class SimAdministrationApplication : Application<SimAdministrationConfiguration>() {

    override fun getName(): String {
        return "SIM inventory application"
    }

    override fun initialize(bootstrap: Bootstrap<SimAdministrationConfiguration>) {
        // TODO: application initialization
    }

    lateinit var DAO: SimInventoryDAO

    override fun run(config: SimAdministrationConfiguration,
                     env: Environment) {
        val factory = DBIFactory()
        val jdbi = factory.build(env,
                config.database, "postgresql")
        this.DAO = jdbi.onDemand(SimInventoryDAO::class.java)

        val profileVendorCallbackHandler = object : SmDpPlusCallbackService {
            // TODO: Not implemented.
            override fun handleDownloadProgressInfo(
                    eid: String?,
                    iccid: String,
                    notificationPointId: Int,
                    profileType: String?,
                    resultData: String?,
                    timestamp: String) = Unit
        }

        val client = JerseyClientBuilder(env)
                .using(config.getJerseyClientConfiguration())
                .build(env.name)
        val jerseyEnv = env.jersey()

        addOpenapiResourceToJerseyEnv(jerseyEnv, config.openApi)
        addEs2PlusDefaultFiltersAndInterceptors(jerseyEnv)

        jerseyEnv.register(SimInventoryResource(client, config, DAO))
        jerseyEnv.register(SmDpPlusCallbackResource(profileVendorCallbackHandler))
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SimAdministrationApplication().run(*args)
        }
    }
}
