package org.ostelco.simcards.admin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.codahale.metrics.health.HealthCheck
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.getLogger
import org.ostelco.prime.simmanager.NotFoundError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.profilevendors.ProfileVendorAdapter
import org.ostelco.simcards.profilevendors.ProfileVendorAdapterDatum
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
        // XXX Currently checking every ten seconds. Every minute or five is more reasonable in a
        //     production setting. Perhaps a config setting is reasonable to regulate this?
        executorService.scheduleAtFixedRate(this::updateStatus, 0, 1, TimeUnit.MINUTES)
    }

    // The last known status
    private val status = AtomicBoolean(false)

    // Do the status check
    private fun updateStatus() {
        val value = checkIfSmdpPlusIsUp()
        this.status.set(value)
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
            return checkIfSmdpPlusIsUpNaively()
        } catch (t: Throwable) {
            logger.error("Something weird happened while checking for SMDP+-es being up.", t)
            return false
        }
    }


    private fun getProfileVendorAdapterForVendor(pvd: ProfileVendorAdapterDatum): Either<SimManagerError, ProfileVendorAdapter> {
        val currentConfig: ProfileVendorConfig? =
                getConfigForVendorWithName(pvd.name)

        if (currentConfig == null) {
            val msg = "Could not find config for profile vendor '${pvd.name}' while attempting to ping remote SM-DP+ adapter"
            logger.error(msg)
            return NotFoundError(msg).left()
        }

        return ProfileVendorAdapter(pvd, currentConfig, httpClient, simInventoryDAO).right()
    }

    private fun checkIfSmdpPlusIsUpNaively(): Boolean {
        val profileVendorAdaptorList = simInventoryDAO.getAllProfileVendors().fold({
            logger.info("Couldn't find any profile vendors: {}", it)
            return false
        }, { it })

        loopOverAllProfileVendors@ for (vendorAdapterDatum in profileVendorAdaptorList) {
            var vendorAdapter = getProfileVendorAdapterForVendor(vendorAdapterDatum).map(
                    {return false}, {foo -> foo})

            // If this was an error, but of an acceptable ("pingOk" == true) kind, meaning that
            // the endpoint in the other end actually gave a reasonable answer to a reasonable request,
            // indicating that the endpoint is answering requests, then continue to loop over next endpoint,
            // otherwise see if there is an error.
            when (val pingResult = vendorAdapter.ping()) {
                is Either.Left -> if (pingResult.a.pingOk) {
                    continue@loopOverAllProfileVendors
                } else {
                    logger.error("Could not reach SM-DP+ via HTTP PING:", pingResult)
                    return false
                }
                is Either.Right -> {
                }
            }
        }
        return true
    }

    private fun getConfigForVendorWithName(vendorName: String) =
            profileVendorConfigList.firstOrNull { it.name == vendorName }
}

