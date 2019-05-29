package org.ostelco.simcards.hss

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.core.flatMap
import arrow.core.right
import com.codahale.metrics.health.HealthCheck
import io.grpc.ManagedChannelBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.simmanager.AdapterError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.admin.HssConfig
import org.ostelco.simcards.admin.mapRight
import org.ostelco.simcards.hss.profilevendors.api.HssServiceGrpc
import org.ostelco.simcards.hss.profilevendors.api.ServiceHealthQuery
import org.ostelco.simcards.inventory.HssState
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

interface HssDispatcher {
    fun name(): String
    fun iAmHealthy(): Boolean
    fun activate(hssName: String, iccid: String, msisdn: String): Either<SimManagerError, Unit>
    fun suspend(hssName: String, iccid: String): Either<SimManagerError, Unit>
}


class HssGrpcAdapter(private val host: String, private val port: Int) : HssDispatcher {


    override fun name(): String {
        return "HssGRPC Adapter connecting to host $host on port $port"
    }

    private var blockingStub: HssServiceGrpc.HssServiceBlockingStub

    init {
        val channel =
                ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext(true)
                        .build()

        this.blockingStub =
                HssServiceGrpc.newBlockingStub(channel)
    }

    fun activateViaGrpc(hssName: String, iccid: String, msisdn: String): Boolean {
        val activationRequest =
                org.ostelco.simcards.hss.profilevendors.api.ActivationRequest.newBuilder()
                        .setIccid(iccid)
                        .setHss(hssName)
                        .setMsisdn(msisdn)
                        .build()
        val response = blockingStub.activate(activationRequest)
        return response.success
    }


    fun suspendViaGrpc(hssName: String, iccid: String): Boolean {
        val suspensionRequest = org.ostelco.simcards.hss.profilevendors.api.SuspensionRequest.newBuilder()
                .setIccid(iccid)
                .setHss(hssName)
                .build()
        val response = blockingStub.suspend(suspensionRequest)
        return response.success
    }

    override fun iAmHealthy(): Boolean {
        val request = ServiceHealthQuery.newBuilder().build()
        val response = blockingStub.getHealthStatus(request)
        return response.isHealthy
    }


    override fun activate(hssName: String, iccid: String, msisdn: String): Either<SimManagerError, Unit> {
        if (activateViaGrpc(hssName = hssName, msisdn = msisdn, iccid = iccid)) {
            return Right(Unit)
        } else {
            return Left(AdapterError("Could not activate via grpc (host=$host, port =$port) for hss = $hssName, msisdn=$msisdn, iccid=$iccid"))
        }

    }

    override fun suspend(hssName: String, iccid: String): Either<SimManagerError, Unit> {
        if (suspendViaGrpc(hssName = hssName, iccid = iccid)) {
            return Right(Unit)
        } else {
            return Left(AdapterError("Could not activate via grpc (host=$host, port =$port) for hss = $hssName, iccid=$iccid"))
        }
    }
}


class DirectHssDispatcher(
        val hssConfigs: List<HssConfig>,
        val httpClient: CloseableHttpClient,
        val healthCheckRegistrar: HealthCheckRegistrar? = null) : HssDispatcher {

    override fun name(): String {
        return "Direct HSS dispatcher serving HSS configurations with names: ${hssConfigs.map { it.name }}"
    }

    val adapters = mutableSetOf<HssDispatcher>()

    private val hssAdaptersByName = mutableMapOf<String, HssDispatcher>()
    private val healthchecks = mutableSetOf<HssDispatcherHealthcheck>()

    init {

        for (config in hssConfigs) {
            adapters.add(SimpleHssDispatcher(name = config.name, httpClient = httpClient, config = config))
        }


        for (adapter in adapters) {

            val healthCheck = HssDispatcherHealthcheck(adapter.name(), adapter)
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

    private fun getHssAdapterByName(name: String): HssDispatcher {
        if (!hssAdaptersByName.containsKey(name)) {
            throw RuntimeException("Unknown hss vendor name ? '$name'")
        }
        return hssAdaptersByName[name]!!
    }

    override fun activate(hssName: String, iccid: String, msisdn: String): Either<SimManagerError, Unit> {
        return getHssAdapterByName(hssName).activate(hssName= hssName, iccid = iccid, msisdn = msisdn)
    }

    override fun suspend(hssName: String, iccid: String): Either<SimManagerError, Unit> {
        return getHssAdapterByName(hssName).suspend(hssName=hssName, iccid = iccid)
    }
}

/**
 * Keep a set of HSS entries that can be used when
 * provisioning SIM profiles in remote HSSes.
 */
class SimManagerToHssDispatcherAdapter(
        val dispatcher: HssDispatcher,
        val simInventoryDAO: SimInventoryDAO)  {

    private val log = LoggerFactory.getLogger(javaClass)

    private val idToNameMap = mutableMapOf<Long, String>()

    private val lock = Object()

    init {
        updateHssIdToNameMap()
    }

    private fun fetchHssEntriesFromDatabase(): List<HssEntry> {
        val returnValue = mutableListOf<HssEntry>()
        // XXX This doesn't look kosher.  Anyone with deep insight into Arrow please suggest a fix.
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
            return dispatcher.activate(hssName = idToNameMap[simEntry.hssId]!!, iccid = simEntry.iccid, msisdn = simEntry.msisdn)
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

class HssDispatcherHealthcheck(
        private val name: String,
        private val entry: HssDispatcher) : HealthCheck() {

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

