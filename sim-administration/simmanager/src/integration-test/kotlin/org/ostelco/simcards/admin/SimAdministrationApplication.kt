package org.ostelco.simcards.admin

import com.codahale.metrics.health.HealthCheck
import io.dropwizard.Application
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.jdbi3.JdbiFactory
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.ostelco.dropwizardutils.OpenapiResourceAdder.Companion.addOpenapiResourceToJerseyEnv
import org.ostelco.sim.es2plus.ES2PlusIncomingHeadersFilter.Companion.addEs2PlusDefaultFiltersAndInterceptors
import org.ostelco.sim.es2plus.SmDpPlusCallbackResource
import org.ostelco.simcards.inventory.SimInventoryCallbackService
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SimInventoryDB
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
        /* Enables ENV variable substitution in config file. */
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
                bootstrap.configurationSourceProvider,
                EnvironmentVariableSubstitutor(false)
        )
    }

    public lateinit var DAO: SimInventoryDAO

    override fun run(config: SimAdministrationConfiguration,
                     env: Environment) {
        val factory = JdbiFactory()
        val jdbi = factory
                .build(env, config.database, "postgresql")
                .installPlugins()
        DAO = SimInventoryDAO(jdbi.onDemand(SimInventoryDB::class.java))

        val profileVendorCallbackHandler = SimInventoryCallbackService(DAO)

        val httpClient = HttpClientBuilder(env)
                .using(config.httpClient)
                .build(name)
        val jerseyEnv = env.jersey()

        addOpenapiResourceToJerseyEnv(jerseyEnv, config.openApi)
        addEs2PlusDefaultFiltersAndInterceptors(jerseyEnv)

        // Add resoures that should be run from the outside via REST.

        jerseyEnv.register(SimInventoryResource(httpClient, config, this.DAO))
        jerseyEnv.register(SmDpPlusCallbackResource(profileVendorCallbackHandler))

        // Add task that should be triggered periodically by external
        // cron job via tasks/preallocate_sim_profiles url.

        val hssAdapters = HssAdapterManager(
                heathCheckRegistrar = object : HealthCheckRegistrar {
                    override fun registerHealthCheck(name: String, healthCheck: HealthCheck) {
                        override fun registerHealthCheck(name: String, healthCheck: HealthCheck) {
                            env.healthChecks().register(name, healthCheck)
                        }
                    }
                }
                hssConfigs = config.hssVendors,
                simInventoryDAO = this.DAO,
                httpClient = httpClient)

        env.admin().addTask(PreallocateProfilesTask(
                hssAdapters =  hssAdapters,
                simInventoryDAO = this.DAO,
                httpClient = httpClient,
                profileVendors = config.profileVendors));
    }
}
