package org.ostelco.simcards.hss

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import com.codahale.metrics.health.HealthCheck
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.admin.HssConfig
import org.ostelco.simcards.admin.mapRight
import org.ostelco.simcards.inventory.HssState
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

interface HssDispatcher {
    fun iAmHealthy(): Boolean
    fun activate(hssName: String, msisdn: String, iccid: String): Either<SimManagerError, Unit>
    fun suspend(hssName: String, iccid: String): Either<SimManagerError, Unit>
}

class DirectHssDispatcher(
        val hssConfigs: List<HssConfig>,
        val httpClient : CloseableHttpClient,
        val healthCheckRegistrar: HealthCheckRegistrar? = null) : HssDispatcher {

    val adapters = mutableSetOf<HssAdapter>()

    private val hssAdaptersByName = mutableMapOf<String, HssAdapter>()
    private val healthchecks = mutableSetOf<HssAdapterHealthcheck>()

    init {

        for (config in hssConfigs) {
            adapters.add(SimpleHssAdapter(name = config.name, httpClient = httpClient, config = config))
        }


        for (adapter in adapters) {

            val healthCheck = HssAdapterHealthcheck(adapter.name(), adapter)
            healthchecks.add(healthCheck)

            healthCheckRegistrar?.registerHealthCheck(
                    "HSS profilevendors for Hss named '${adapter.name()}'",
                    healthCheck)

            hssAdaptersByName[adapter.name()] = adapter
        }
    }

    // NOTE! Assumes that healthchecks on private hss entries are being run
    // periodically and can therefore be considered to be updated & valid.
    override fun iAmHealthy(): Boolean {
        return healthchecks
                .map { it.getLastHealthStatus() }
                .reduce { a, b -> a && b }
    }

    private fun getHssAdapterByName(name: String): HssAdapter {
        if (!hssAdaptersByName.containsKey(name)) {
            throw RuntimeException("Unknown hss profilevendors name ? '$name'")
        }
        return hssAdaptersByName[name]!!
    }

    override fun activate(hssName: String, msisdn: String, iccid: String): Either<SimManagerError, Unit> {
        return getHssAdapterByName(hssName).activate(msisdn = msisdn, iccid = iccid)
    }

    override fun suspend(hssName: String, iccid: String): Either<SimManagerError, Unit> {
        return getHssAdapterByName(hssName).suspend(iccid = iccid)
    }
}

/**
 * Keep a set of HSS entries that can be used when
 * provisioning SIM profiles in remote HSSes.
 */
class HssProxy(
        val dispatcher: HssDispatcher,
        val simInventoryDAO: SimInventoryDAO) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val idToNameMap = mutableMapOf<Long, String>()

    private val lock = Object()

    init {
        updateHssIdToNameMap()
    }

    private fun fetchHssEntriesFromDatabase(): List<HssEntry> {
        val returnValue = mutableListOf<HssEntry>()
        val entries = simInventoryDAO.getHssEntries()
                .mapLeft { err ->
                    log.error("No HSS entries to be found by the DAO.")
                    log.error(err.description)
                }
                .mapRight { returnValue.addAll(it) }
        return returnValue
    }

    private fun updateHssIdToNameMap() {
        synchronized(lock) {

            val newHssEntries =
                    fetchHssEntriesFromDatabase()
                            .filter { !idToNameMap.containsValue(it.name) }

            for (newHssEntry in newHssEntries) {
                idToNameMap[newHssEntry.id] = newHssEntry.name
            }
        }
    }

    fun activate(simEntry: SimEntry): Either<SimManagerError, Unit> {
        synchronized(lock) {
            return dispatcher.activate(hssName = idToNameMap[simEntry.hssId]!!, msisdn = simEntry.msisdn, iccid = simEntry.iccid)
                    .flatMap { simInventoryDAO.setHssState(simEntry.id!!, HssState.ACTIVATED) }
                    .flatMap { Unit.right() }
        }
    }

    fun suspend(simEntry: SimEntry): Either<SimManagerError, Unit> {
        synchronized(lock) {
            return dispatcher.suspend(hssName = idToNameMap[simEntry.hssId]!!, iccid = simEntry.iccid)
                    .flatMap { simInventoryDAO.setHssState(simEntry.id!!, HssState.NOT_ACTIVATED) }
                    .flatMap { Unit.right() }
        }
    }
}

interface HealthCheckRegistrar {
    fun registerHealthCheck(name: String, healthCheck: HealthCheck)
}

class HssAdapterHealthcheck(
        private val name: String,
        private val entry: HssAdapter) : HealthCheck() {

    private val lastHealthStatus = AtomicBoolean(false)

    fun getLastHealthStatus(): Boolean {
        return lastHealthStatus.get()
    }

    @Throws(Exception::class)
    override fun check(): Result {
        return if (entry.iAmHealthy()) {
            lastHealthStatus.set(true)
            Result.healthy()
        } else {
            lastHealthStatus.set(false)
            Result.unhealthy("HSS entry $name is not healthy")
        }
    }
}

