package org.ostelco.simcards.admin

import com.codahale.metrics.health.HealthCheck
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.simcards.adapter.HssAdapter
import org.ostelco.simcards.adapter.HssEntry
import org.ostelco.simcards.adapter.Wg2HssAdapter
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.slf4j.LoggerFactory


/**
 * Keep a set of HSS entries that can be used when
 * provisioning SIM profiles in remote HSSes.
 */
class HssAdapterManager(
        val hssConfigs: List<HssConfig>,
        val simInventoryDAO: SimInventoryDAO,
        val httpClient: CloseableHttpClient,
        val heathCheckRegistrar: HealthCheckRegistrar? = null) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val lock = Object()
    lateinit private var hssEntries: Collection<HssEntry>
    private val hssAdaptersByName = mutableMapOf<String, HssAdapter>()
    private val hssAdaptersById = mutableMapOf<Long, HssAdapter>()
    private val healthchecks = mutableSetOf<HssAdapterHealthcheck>()

    init {
        initialize()
    }

    fun getHssEntries(): Collection<HssEntry> {
        synchronized(lock) {
            return hssEntries
        }
    }

    fun initialize() {
        synchronized(lock) {
            this.hssEntries  = simInventoryDAO.getHssEntries()

            if (this.hssEntries.isNullOrEmpty()) {
                log.error("No HSS entries to be found by the DAO")
                return@synchronized
            }

            hssConfigs.forEach {hssConfig ->
                if (!hssAdaptersByName.containsKey(hssConfig.name)) {

                    // TODO:  This extension point must be able to cater to multiple types
                    //        of adaptes, not just the WG2 type.
                    val adapter = Wg2HssAdapter(httpClient, config = hssConfig, dao = simInventoryDAO)

                    hssAdaptersByName.put(hssConfig.name, adapter)
                    val entryWithName = hssEntries.find { hssEntry -> hssConfig.name == hssEntry.name }
                    if (entryWithName != null) {
                        hssAdaptersById.put(entryWithName!!.id, adapter)


                        if (heathCheckRegistrar != null) {
                            heathCheckRegistrar.registerHealthCheck(
                                    "HSS adapter for Hss named '${hssConfig.name}'",
                                    HssAdapterHealthcheck(adapter))
                        }
                    } else {
                        log.error("Could not find hss entry in database with name '${hssConfig.name}'")
                        return@synchronized
                    }
                }
            }
        }
    }

    fun getHssAdapterByName(name: String): HssAdapter? {
        synchronized(lock) {
            return hssAdaptersByName[name]
        }
    }

    fun getHssAdapterById(id: Long): HssAdapter? {
        synchronized(lock) {
            return hssAdaptersById[id]
        }
    }
}


interface HealthCheckRegistrar {
    fun registerHealthCheck(name: String, healthCheck: HealthCheck)
}


class HssAdapterHealthcheck(private val adapter: HssAdapter) : HealthCheck() {

    @Throws(Exception::class)
    override fun check(): Result {
        return if (adapter.iAmHealthy()) {
            Result.healthy()
        } else Result.unhealthy("HSS adapter ${adapter.getName()} is not healthy")
    }
}