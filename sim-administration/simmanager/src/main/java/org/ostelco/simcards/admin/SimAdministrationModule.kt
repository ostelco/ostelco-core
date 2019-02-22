package org.ostelco.simcards.admin

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.jdbi3.JdbiFactory
import io.dropwizard.setup.Environment
import org.ostelco.dropwizardutils.OpenapiResourceAdder
import org.ostelco.prime.module.PrimeModule
import org.ostelco.sim.es2plus.ES2PlusIncomingHeadersFilter
import org.ostelco.sim.es2plus.SmDpPlusCallbackResource
import org.ostelco.sim.es2plus.SmDpPlusCallbackService
import org.ostelco.simcards.admin.ConfigRegistry.config
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SimInventoryResource

/**
 * The SIM manager
 * is an component that inputs inhales SIM batches
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
@JsonTypeName("sim-manager")
class SimAdministrationModule : PrimeModule {

    lateinit var DAO: SimInventoryDAO

    @JsonProperty("config")
    fun setConfig(config: SimAdministrationConfiguration) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        val factory = JdbiFactory()
        val jdbi = factory.build(env,
                config.database, "postgresql")
                .installPlugins()
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

        val httpClient = HttpClientBuilder(env)
                .using(config.httpClient)
                .build("SIM inventory")
        val jerseyEnv = env.jersey()

        OpenapiResourceAdder.addOpenapiResourceToJerseyEnv(jerseyEnv, config.openApi)
        ES2PlusIncomingHeadersFilter.addEs2PlusDefaultFiltersAndInterceptors(jerseyEnv)

        jerseyEnv.register(SimInventoryResource(httpClient, config, DAO))
        jerseyEnv.register(SmDpPlusCallbackResource(profileVendorCallbackHandler))

        env.admin().addTask(PreallocateProfilesTask(
                simInventoryDAO = this.DAO,
                httpClient = httpClient,
                hlrConfigs = config.hlrVendors,
                profileVendors = config.profileVendors));
    }
}

object ConfigRegistry {
    lateinit var config: SimAdministrationConfiguration
}