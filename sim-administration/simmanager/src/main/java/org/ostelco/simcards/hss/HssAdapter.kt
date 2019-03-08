package org.ostelco.simcards.hss

import com.codahale.metrics.health.HealthCheck
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.simcards.admin.HssConfig
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Keep a set of HSS entries that can be used when
 * provisioning SIM profiles in remote HSSes.
 */
class HssProxy(
        val hssConfigs: List<HssConfig>,
        val simInventoryDAO: SimInventoryDAO,
        val httpClient: CloseableHttpClient,
        val heathCheckRegistrar: HealthCheckRegistrar? = null) : HssAdapter {

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

    // NOTE! Assumes that healthchecks on private hss entries are being run
    // periodically and can therefore be considered to be updated & valid.
    override fun iAmHealthy(): Boolean {
        return healthchecks
                .map { it.getLastHealthStatus() }
                .reduce{ a, b -> a && b}
    }

    fun initialize() {
        synchronized(lock) {
            this.hssEntries = simInventoryDAO.getHssEntries()

            if (this.hssEntries.isNullOrEmpty()) {
                log.error("No HSS entries to be found by the DAO")
                return@synchronized
            }

            hssConfigs.forEach { hssConfig ->
                if (!hssAdaptersByName.containsKey(hssConfig.name)) {

                    // TODO:  This extension point must be able to cater to multiple types
                    //        of adapter.
                    val adapter = SimpleHssAdapter(httpClient, config = hssConfig, dao = simInventoryDAO)

                    hssAdaptersByName.put(hssConfig.name, adapter)
                    val entryWithName = hssEntries.find { hssEntry -> hssConfig.name == hssEntry.name }
                    if (entryWithName != null) {
                        hssAdaptersById.put(entryWithName!!.id, adapter)


                        if (heathCheckRegistrar != null) {
                            heathCheckRegistrar.registerHealthCheck(
                                    "HSS adapter for Hss named '${hssConfig.name}'",
                                    HssAdapterHealthcheck(hssConfig.name, adapter))
                        }
                    } else {
                        log.error("Could not find hss entry in database with name '${hssConfig.name}'")
                        return@synchronized
                    }
                }
            }
        }
    }

    private fun getHssAdapterByName(name: String): HssAdapter {
        synchronized(lock) {
            if (!hssAdaptersByName.containsKey(name)) {
                throw RuntimeException("Unknown hss adapter name ? '$name'")
            }
            return hssAdaptersByName[name]!!
        }
    }

    private fun getHssAdapterById(id: Long): HssAdapter {
        synchronized(lock) {
            if (!hssAdaptersById.containsKey(id)) {
                throw RuntimeException("Unknown hss adapter id ? '$id'")
            }
            return hssAdaptersById[id]!!
        }
    }


    override fun activate(simEntry: SimEntry) {
        return getHssAdapterById(simEntry.hssId).activate(simEntry)
    }

    override fun suspend(simEntry: SimEntry) {
        return getHssAdapterById(simEntry.hssId).suspend(simEntry)
    }
}


interface HealthCheckRegistrar {
    fun registerHealthCheck(name: String, healthCheck: HealthCheck)
}


class HssAdapterHealthcheck(
        private val name: String,
        private val adapter: HssAdapter) : HealthCheck() {

    private val lastHealthStatus = AtomicBoolean(false)

    fun getLastHealthStatus():Boolean {
        return lastHealthStatus.get()
    }

    @Throws(Exception::class)
    override fun check(): Result {
        return if (adapter.iAmHealthy()) {
            lastHealthStatus.set(true)
            Result.healthy()
        } else {
            lastHealthStatus.set(false)
            Result.unhealthy("HSS adapter ${name} is not healthy")
        }
    }
}