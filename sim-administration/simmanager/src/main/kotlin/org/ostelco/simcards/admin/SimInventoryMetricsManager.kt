package org.ostelco.simcards.admin

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import io.dropwizard.lifecycle.Managed
import org.ostelco.prime.getLogger
import org.ostelco.simcards.inventory.SimInventoryDAO
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


class SimInventoryMetricsManager(private val dao: SimInventoryDAO, metrics: MetricRegistry) : Managed {

    ///
    ///  Set up the metrics manager and prepare to run periodic task every five minutes.
    ///

    private val metricsRegistry = LocalMetricsRegistry(metrics)
    private val logger by getLogger()
    private val executorService = Executors.newScheduledThreadPool(5)
    private val isRunning = AtomicBoolean(false)
    private val hasRun = AtomicBoolean(false)


    /**
     *  Start execution service.  Do not permit restarts even of stopped instances.
     */
    override fun start() {
        logger.warn("Starting metrics for sim inventory")
        if (!isRunning.getAndSet(true)) {
            if (hasRun.getAndSet(true)) {
                throw RuntimeException("Attempt to start an already started instance.")
            }
            startExecutorService()
        }
    }

    /**
     *  Stop when dropwizard is killed
     */
    override fun stop() {
        if (isRunning.getAndSet(false)) {
            executorService.shutdownNow()
        }
    }

    /**
     * Start an execution service that will run the periodicTask method every
     * five minutes.
     */
    private fun startExecutorService() {
        executorService.scheduleAtFixedRate({
            if (isRunning.get()) {
                try {
                    periodicTask()
                } catch (t: Throwable) {
                    logger.warn("Periodic sim inventory metrics poller failed", t)
                }
            }
        }, 0L, 300L, TimeUnit.SECONDS)
    }


    ///
    /// Handle the periodic task.
    ///

    private fun periodicTask() {

        logger.info("Periodic sim inventory metrics poller executing")

        val metricValues: Collection<MetricValue> = getMetricsValues()
        metricsRegistry.syncWithValues(metricValues)
    }

    private fun getMetricsValues(): Collection<MetricValue> {

        val result = mutableListOf<MetricValue>()

        dao.getHssProfileNamePairs()
                .mapRight { pairsToQuery ->
                    pairsToQuery.forEach { currentMetric ->
                        dao.getProfileStats(currentMetric.hssId, currentMetric.simProfileName)
                                .mapRight {
                                    result.add(MetricValue("sims.noOfEntries", currentMetric.simProfileName, it.noOfEntries))
                                    result.add(MetricValue("sims.noOfEntriesAvailableForImmediateUse", currentMetric.simProfileName, it.noOfEntriesAvailableForImmediateUse))
                                    result.add(MetricValue("sims.noOfReleasedEntries", currentMetric.simProfileName, it.noOfReleasedEntries))
                                    result.add(MetricValue("sims.noOfUnallocatedEntries", currentMetric.simProfileName, it.noOfUnallocatedEntries))
                                    result.add(MetricValue("sims.noOfReservedEntries", currentMetric.simProfileName, it.noOfReservedEntries))
                                }
                    }
                }

        return result
    }

    /**
     *  This method is presenent only to facilitate testing. It won't hurt to invoke it
     *  in other situations, but it's not really inteded to be used that way.
     */
    fun triggerMetricsGeneration() {
        periodicTask()
    }
}


/**
 * A class used to hold local metric values, and to connect them
 * to dropwizard metrics that can be polled via the ordinary metrics
 * export mechanisms of dropwizard.
 */
class LocalMetricsRegistry(private val metrics: MetricRegistry) {

    private val lock = Object()

    /**
     * The set of metrics holding the current values.
     */
    val localMetrics = mutableMapOf<String, LocalGaugeAdapter>()


    /**
     * Input is current metric values as dictated  by structure and
     * content of database.
     */
    fun syncWithValues(metricValues: Collection<MetricValue>) {

        //    * Based on this collection, prune and extend the set of
        //      metrics that are being mainained.
        //    * Inject the current metric values into the actual
        //      metrics that are transmitted via the metrics mechanism.

        synchronized(lock) {
            val currentMetricNames = mutableSetOf<String>()
            metricValues.forEach { currentValue ->
                val key: String = getMetricName(currentValue)
                val value = localMetrics[key]
                currentMetricNames.add(key)

                if (value == null) {
                    localMetrics[key] = LocalGaugeAdapter(key, currentValue.value)
                    metrics.register(key, localMetrics[key])
                } else {
                    value.updateValue(currentValue.value)
                }
            }

            val irrelevantMetrics = localMetrics.keys.subtract(currentMetricNames)
            irrelevantMetrics.forEach {
                metrics.remove(it)
                localMetrics.remove(it)
            }
        }
    }

    private fun getMetricName(it: MetricValue): String {
        return "${it.metricName}.${it.profileName}"
    }
}

/**
 * An ad-hoc class that is used  to deliver values from local values to
 * externally visible valus.
 */
class LocalGaugeAdapter(private val key: String, initialValue: Long) : Gauge<Long> {

    val currentValue = AtomicLong(initialValue)

    override fun getValue(): Long {
        return currentValue.get()
    }

    fun updateValue(value: Long) {
        currentValue.set(value)
    }
}

/**
 * Representing values associated with metrics, before they are sent to
 * the metric.
 */
data class MetricValue(
        val metricName: String,
        val profileName: String, // Name of the SIM profile, will somehow need to be transmitted to prometheus.
        val value: Long)
