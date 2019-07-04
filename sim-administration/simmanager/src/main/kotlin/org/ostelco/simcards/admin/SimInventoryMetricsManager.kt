package org.ostelco.simcards.admin

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import io.dropwizard.lifecycle.Managed
import org.ostelco.prime.getLogger
import org.ostelco.simcards.inventory.SimInventoryDAO
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


class SimInventoryMetricsManager(private val dao: SimInventoryDAO, private val metrics: MetricRegistry) : Managed {
    
    ///
    ///  Set up the metrics manager and prepare to run periodic task every five minutes.
    ///
    
    private val metricsRegistry = LocalMetricsRegistry(metrics)
    private val logger by getLogger()
    private val executorService = Executors.newScheduledThreadPool(5)
    private val isRunning = AtomicBoolean(false)
    private val hasRun = AtomicBoolean(false)


    // Start execution service.  Do not permit restarts even of stopped instances.
    override fun start() {
        logger.warn("Starting metrics for sim inventory")
        if (!isRunning.getAndSet(true)) {
            if (hasRun.getAndSet(true)) {
                throw RuntimeException("Attempt to start an already started instance.")
            }
            startExecutorService()
        }
    }

    // Stop when dropwizard is killed
    override fun stop() {
        if (isRunning.getAndSet(false)) {
            executorService.shutdownNow()
        }
    }

    // Start an execution service that will run the periodicTask method every
    // five minutes.
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


    val runningForFirstTime = AtomicBoolean(true)

    private fun periodicTask() {

        logger.info("Periodic sim inventory metrics poller executing")


        // XXX Just a little dummy thing we need until we are creating proper metrics,
        //     so that we can see that metrics are in fact being produced in other parts
        //     of the system.
        if (runningForFirstTime.getAndSet(false)) {
            // Create a dummy metric, will later be done by the periodic task
            // when needed.
            // SimInventoryMetricsManager::class.java, "dummyMetric", "noOfThings")
            metrics.register("dummyMetric.do.ignore",
                    object : Gauge<Int> {
                        override fun getValue(): Int {
                            return 42
                        }
                    })
        }


        val metricValues: Collection<MetricValue>  = getMetricsValues()
        metricsRegistry.syncWithValues(metricValues)
    }

    private fun getMetricsValues(): Collection<MetricValue> {
        return listOf()
    }
}

class LocalMetricsRegistry(private val metrics: MetricRegistry) {

    private val lock = Object()

    val localMetrics = mutableMapOf<String, LocalGaugeAdapter>()

    // Game plan.   For each invocation:
    //    * Get current metric values as dictated  by structure and
    //      content of database.
    //    * Based on this collection, prune and extend the set of
    //      metrics that are being mainained.
    //    * Inject the current metric values into the actual
    //      metrics that are transmitted via the metrics mechanism.

    fun syncWithValues(metricValues: Collection<MetricValue>) {
        synchronized(lock) {
            val currentMetrics = metrics.metrics
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

            val irrelevantMetrics = currentMetrics.keys.subtract(currentMetricNames)
            irrelevantMetrics.forEach{
                metrics.remove(it)
                localMetrics.remove(it)
            }
        }
    }


    private fun getMetricName(it: MetricValue): String {
            return "${it.metricName}.${it.profileName}"
    }
}

class LocalGaugeAdapter(private val key: String, private var initialValue: Int) : Gauge<Int> {

    val currentValue = AtomicInteger(initialValue)

    override fun getValue(): Int {
        return currentValue.get()
    }

    fun updateValue(value: Int) {
        currentValue.set(value)
    }
}

// XXX This is representing a concrete gauge, but needs to
//     be abstracted in a couple of steps.
data class MetricValue (
        val metricName: String,
        val profileName: String, // Name of the SIM profile, will somehow need to be transmitted to prometheus.
        val value: Int)
