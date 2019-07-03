package org.ostelco.simcards.admin

import com.codahale.metrics.MetricRegistry
import io.dropwizard.lifecycle.Managed
import org.ostelco.prime.getLogger
import org.ostelco.simcards.inventory.SimInventoryDAO
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


class SimInventoryMetricsManager(dao: SimInventoryDAO, metrics: MetricRegistry) : Managed {

    private val logger by getLogger()
    val executorService = Executors.newScheduledThreadPool(5)
    val isRunning = AtomicBoolean(false)


    // Run every five minutes
    override fun start() {
        if (!isRunning.getAndSet(true)) {
            executorService.scheduleAtFixedRate({
                if (isRunning.get()) {
                    try {
                        logger.info("Periodic sim inventory metrics poller executing")
                    } catch (Throwable t) {
                        logger.warn("Periodic inventory metrics poller failed", t)
                    }
                }
            }, 0L, 300L, TimeUnit.SECONDS)
        }
    }

    override fun stop() {
        if (isRunning.getAndSet(false)) {
            executorService.shutdownNow()
        }
    }
}
