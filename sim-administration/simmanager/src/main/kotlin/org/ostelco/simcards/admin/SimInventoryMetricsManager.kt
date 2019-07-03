package org.ostelco.simcards.admin

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import io.dropwizard.lifecycle.Managed
import org.ostelco.prime.getLogger
import org.ostelco.simcards.inventory.SimInventoryDAO
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


class SimInventoryMetricsManager(private val dao: SimInventoryDAO, private val metrics: MetricRegistry) : Managed {


    ///
    ///  Set up the metrics manager and prepare to run periodic task every five minutes.
    ///

    private val logger by getLogger()
    val executorService = Executors.newScheduledThreadPool(5)
    val isRunning = AtomicBoolean(false)


    // Run every five minutes
    override fun start() {
        if (!isRunning.getAndSet(true)) {
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

        if (runningForFirstTime.getAndSet(false)) {
            // Create a dummy metric, will later be done by the periodic task
            // when needed.
            metrics.register(MetricRegistry.name(SimInventoryMetricsManager::class.java, "dummyMetric", "noOfThings"),
                    object : Gauge<Int> {
                        override fun getValue(): Int {
                            return 42
                        }
                    })
        }
    }
}
