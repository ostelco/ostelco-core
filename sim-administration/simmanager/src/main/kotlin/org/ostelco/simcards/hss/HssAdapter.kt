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

// TODO This file still contains an uncomfortable mix of arrow and
//      straight kotlin.  Need to resolve this one way or another.

class HssDispatcher(val adapters: Set<HssAdapter>, val healthCheckRegistrar: HealthCheckRegistrar? = null) {

    private val hssAdaptersByName = mutableMapOf<String, HssAdapter>()
    private val healthchecks = mutableSetOf<HssAdapterHealthcheck>()

    init {
        for (adapter in adapters) {

            healthCheckRegistrar?.registerHealthCheck(
                    "HSS adapter for Hss named '${adapter.name()}'",
                    HssAdapterHealthcheck(adapter.name(), adapter))

            hssAdaptersByName[adapter.name()] = adapter
        }
    }

    // NOTE! Assumes that healthchecks on private hss entries are being run
    // periodically and can therefore be considered to be updated & valid.
    fun iAmHealthy(): Boolean {
        return healthchecks
                .map { it.getLastHealthStatus() }
                .reduce { a, b -> a && b }
    }

    private fun getHssAdapterByName(name: String): HssAdapter {
        if (!hssAdaptersByName.containsKey(name)) {
            throw RuntimeException("Unknown hss adapter name ? '$name'")
        }
        return hssAdaptersByName[name]!!
    }

    fun activate(hssName: String, msisdn: String, iccid: String): Either<SimManagerError, Unit> {
        return getHssAdapterByName(hssName).activate(msisdn = msisdn, iccid = iccid)
    }

    fun suspend(hssName: String, iccid: String): Either<SimManagerError, Unit> {
        return getHssAdapterByName(hssName).activate(iccid = iccid)
    }
}

/**
 * Keep a set of HSS entries that can be used when
 * provisioning SIM profiles in remote HSSes.
 */
class HssProxy(
        val hssConfigs: List<HssConfig>,
        val simInventoryDAO: SimInventoryDAO,
        val httpClient: CloseableHttpClient,
        val healthCheckRegistrar: HealthCheckRegistrar? = null) {

    private val log = LoggerFactory.getLogger(javaClass)


    private val idToNameMap = mutableMapOf<Long, String>()

    private val dispatcher: HssDispatcher

    init {

        val adapters = mutableSetOf<HssAdapter>()

        for (config in hssConfigs) {
            adapters.add(SimpleHssAdapter(name = config.name, httpClient = httpClient, config = config))
        }

        dispatcher = HssDispatcher(adapters = adapters, healthCheckRegistrar = healthCheckRegistrar)

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

    fun getHssConfigFor(name: String): HssConfig {
        return hssConfigs.singleOrNull() { it.name == name }!! // TODO: Fail if null!
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

