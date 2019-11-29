package org.ostelco.simcards.admin

import ch.qos.logback.access.servlet.TeeFilter
import com.codahale.metrics.health.HealthCheck
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.jdbi3.JdbiFactory
import io.dropwizard.setup.Environment
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.dropwizardutils.OpenapiResourceAdder
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.SimProfileStatus
import org.ostelco.prime.module.PrimeModule
import org.ostelco.sim.es2plus.ES2PlusIncomingHeadersFilter
import org.ostelco.sim.es2plus.SmDpPlusCallbackResource
import org.ostelco.simcards.admin.ApiRegistry.simInventoryApi
import org.ostelco.simcards.admin.ConfigRegistry.config
import org.ostelco.simcards.admin.ResourceRegistry.simInventoryResource
import org.ostelco.simcards.hss.DirectHssDispatcher
import org.ostelco.simcards.hss.DummyHSSDispatcher
import org.ostelco.simcards.hss.HealthCheckRegistrar
import org.ostelco.simcards.hss.HssDispatcher
import org.ostelco.simcards.hss.HssGrpcAdapter
import org.ostelco.simcards.hss.SimManagerToHssDispatcherAdapter
import org.ostelco.simcards.hss.SimpleHssDispatcher
import org.ostelco.simcards.inventory.SimInventoryApi
import org.ostelco.simcards.inventory.SimInventoryCallbackService
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SimInventoryDB
import org.ostelco.simcards.inventory.SimInventoryDBWrapperImpl
import org.ostelco.simcards.resources.SimInventoryResource

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

    private val logger by getLogger()

    private lateinit var metricsManager: SimInventoryMetricsManager

    private lateinit var DAO: SimInventoryDAO

    @JsonProperty("config")
    fun setConfig(config: SimAdministrationConfiguration) {
        ConfigRegistry.config = config
    }

    fun getDAO() = DAO

    override fun init(env: Environment) {

        logger.info("Initializing Sim administration module.")

        val factory = JdbiFactory()
        val jdbi = factory.build(env,
                config.database, "postgresql")
                .installPlugins()
        DAO = SimInventoryDAO(SimInventoryDBWrapperImpl(jdbi.onDemand(SimInventoryDB::class.java)))

        val profileVendorCallbackHandler = SimInventoryCallbackService(DAO)

        val httpClient = HttpClientBuilder(env)
                .using(config.httpClient)
                .build("mainHttpClient")
        val jerseyEnv = env.jersey()

        OpenapiResourceAdder.addOpenapiResourceToJerseyEnv(jerseyEnv, config.openApi)
        ES2PlusIncomingHeadersFilter.addEs2PlusDefaultFiltersAndInterceptors(jerseyEnv)

        //Create the SIM manager API.
        simInventoryApi = SimInventoryApi(httpClient, config, DAO)

        // Add REST frontend.
        simInventoryResource = SimInventoryResource(simInventoryApi)
        jerseyEnv.register(simInventoryResource)
        jerseyEnv.register(SmDpPlusCallbackResource(profileVendorCallbackHandler))

        // Register Sim Inventory metrics as a lifecycle object

        this.metricsManager = SimInventoryMetricsManager(this.DAO, env.metrics())
        env.lifecycle().manage(this.metricsManager)

        val dispatcher = makeHssDispatcher(
                hssAdapterConfig = config.hssAdapter,
                hssVendorConfigs = config.hssVendors,
                httpClient = httpClient,
                healthCheckRegistrar = object : HealthCheckRegistrar {
                    override fun registerHealthCheck(name: String, healthCheck: HealthCheck) {
                        env.healthChecks().register(name, healthCheck)
                    }
                })

        val hssAdapters = SimManagerToHssDispatcherAdapter(
                dispatcher = dispatcher,
                simInventoryDAO = this.DAO
        )

        env.admin().addTask(PreallocateProfilesTask(
                simInventoryDAO = this.DAO,
                httpClient = httpClient,
                hssAdapterProxy = hssAdapters,
                profileVendors = config.profileVendors))


        env.healthChecks().register("smdp",
                SmdpPlusHealthceck(getDAO(), httpClient, config.profileVendors))

        // logging request and response contents
        env.servlets()
                .addFilter("teeFilter", TeeFilter::class.java)
                .addMappingForUrlPatterns(
                        null,
                        false,
                        "/gsma/rsp2/es2plus/handleDownloadProgressInfo")
    }


    // XXX Implement a feature-flag so that when we want to switch from built in
    //     direct access to HSSes, to adapter-mediated access, we can do that easily
    //     via config.
    private fun makeHssDispatcher(
            hssAdapterConfig: HssAdapterConfig?,
            hssVendorConfigs: List<HssConfig>,
            httpClient: CloseableHttpClient,
            healthCheckRegistrar: HealthCheckRegistrar): HssDispatcher {

        when {
            hssAdapterConfig != null -> {
                return HssGrpcAdapter(
                        host = hssAdapterConfig.hostname,
                        port = hssAdapterConfig.port)
            }
            hssVendorConfigs.isNotEmpty() -> {

                val dispatchers = mutableSetOf<HssDispatcher>()

                for (hssConfig in config.hssVendors) {
                    when (hssConfig) {
                        is SwtHssConfig -> {

                            if (!isLowerCase(hssConfig.hssNameUsedInAPI)) {
                                throw RuntimeException("hssNameUsedInAPI ('${hssConfig.hssNameUsedInAPI}' is not lowercase, this is a syntax error, aborting.")
                            }
                            dispatchers.add(
                                    SimpleHssDispatcher(
                                            name = hssConfig.hssNameUsedInAPI,
                                            httpClient = httpClient,
                                            config = hssConfig
                                    )
                            )
                        }
                        is DummyHssConfig -> {
                            dispatchers.add(
                                    DummyHSSDispatcher(
                                            name = hssConfig.name
                                    )
                            )
                        }
                    }
                }

                return DirectHssDispatcher(
                        hssConfigs = config.hssVendors,
                        httpClient = httpClient,
                        healthCheckRegistrar = healthCheckRegistrar)
            }
            else -> {
                throw RuntimeException("Unable to find HSS adapter config, please check config")
            }
        }
    }

    fun triggerMetricsGeneration() {
        metricsManager.triggerMetricsGeneration()
    }
}

fun isLowerCase(str: String): Boolean {
    return str.toLowerCase().equals(str)
}

object ConfigRegistry {
    lateinit var config: SimAdministrationConfiguration
}

object ResourceRegistry {
    lateinit var simInventoryResource: SimInventoryResource
}

object ApiRegistry {
    lateinit var simInventoryApi: SimInventoryApi
    val simProfileStatusUpdateListeners = mutableSetOf<(iccId: String, status: SimProfileStatus) -> Unit>()
}
