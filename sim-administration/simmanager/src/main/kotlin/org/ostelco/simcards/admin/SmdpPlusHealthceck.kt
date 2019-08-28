package org.ostelco.simcards.admin

import arrow.core.Either
import arrow.core.fix
import arrow.effects.IO
import arrow.instances.either.monad.monad
import com.codahale.metrics.health.HealthCheck
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.getLogger
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.inventory.SimInventoryDAO
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Check if the SMDP+ is available by probing the ES2+ interface for each of  the
 * available sim profile vendors known to the database.
 */

class SmdpPlusHealthceck(
        private val simInventoryDAO: SimInventoryDAO,
        private val httpClient: CloseableHttpClient,
        private val profileVendorConfigList: List<ProfileVendorConfig>) : HealthCheck() {

    private val logger by getLogger()


    // Set up a periodic task that will poll the SM-DP+ instances every minute
    // and update the status based on that info.
    init {
        val executorService = Executors.newSingleThreadScheduledExecutor()
        executorService.scheduleAtFixedRate( this::updateStatus, 0, 1, TimeUnit.MINUTES)
    }

    // The last known status
    private val status = AtomicBoolean(false)

    // Do the status check
    private fun updateStatus() {
        this.status.set(checkIfSmdpPlusIsUp())
    }

    // Based on the last polled value, return a health status.
    @Throws(Exception::class)
    override fun check(): HealthCheck.Result {
        return if (status.get()) {
            HealthCheck.Result.healthy()
        } else HealthCheck.Result.unhealthy("Can't ping SM-DP+")
    }

    /**
     * Contact the available SM-DP+ instances, return true if they are all available, otherwise false.
     */
    private fun checkIfSmdpPlusIsUp(): Boolean {
        try {
            return IO {
                Either.monad<SimManagerError>().binding {
                    val profileVendorAdaptorList = simInventoryDAO.getAllProfileVendors().bind()
                    for (profileVendor in profileVendorAdaptorList) {
                        val currentConfig = profileVendorConfigList.first { it.name == profileVendor.name }

                        // This isn't working very well in the acceptance tests, so we need to log a little.
                        logger.info("About to ping config: $currentConfig")
                        profileVendor.ping(
                                httpClient = httpClient,
                                config = currentConfig
                        ).bind()
                    }
                }.fix()
            }.unsafeRunSync().isRight()
        } catch (t: Throwable) {
            return false
        }
    }
}

