package org.ostelco.prime.ocs

import com.google.common.base.Preconditions
import com.lmax.disruptor.EventHandler
import org.ostelco.prime.disruptor.PrimeEvent
import org.ostelco.prime.disruptor.PrimeEventMessageType
import org.ostelco.prime.logger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.legacy.Storage
import java.util.*

/**
 * For unit testing, loadSubscriberInfo = false
 */
class OcsState(val loadSubscriberInfo:Boolean = true) : EventHandler<PrimeEvent> {

    private val LOG by logger()

    private val dataPackMap = HashMap<String, Long>()
    private val bucketReservedMap = HashMap<String, Long>()

    override fun onEvent(
            event: PrimeEvent,
            sequence: Long,
            endOfBatch: Boolean) {
        try {
            val msisdn = event.msisdn
            if (msisdn == null) {
                LOG.error("Received null as msisdn")
                return
            }
            when (event.messageType) {
                PrimeEventMessageType.CREDIT_CONTROL_REQUEST -> {
                    consumeDataBytes(msisdn, event.usedBucketBytes)
                    event.reservedBucketBytes = reserveDataBytes(
                            msisdn,
                            event.requestedBucketBytes)
                    event.bundleBytes = getDataBundleBytes(msisdn)
                }
                PrimeEventMessageType.TOPUP_DATA_BUNDLE_BALANCE -> event.bundleBytes = addDataBundleBytes(msisdn, event.requestedBucketBytes)
                PrimeEventMessageType.GET_DATA_BUNDLE_BALANCE -> event.bundleBytes = getDataBundleBytes(msisdn)
                PrimeEventMessageType.RELEASE_RESERVED_BUCKET -> releaseReservedBucket(msisdn)
            }
        } catch (e: Exception) {
            LOG.warn("Exception handling prime event in OcsState", e)
        }
    }

    /**
     * Get subscriber's data bundle balance in bytes.
     *
     * @param msisdn Phone number
     * @return bytes Number of bytes available to this number
     * data bundle balance in bytes
     */
    fun getDataBundleBytes(msisdn: String): Long {
        return dataPackMap.getOrDefault(msisdn, 0L)
    }

    /**
     * Add to subscriber's data bundle balance in bytes.
     * This is called when subscriber top ups or, P-GW returns
     * unused data after subscriber disconnects data.
     *
     * @param msisdn Phone number
     * @param bytes Number of bytes we want to add
     * @return bytes data bundle balance in bytes
     */
    fun addDataBundleBytes(msisdn: String, bytes: Long): Long {

        Preconditions.checkArgument(bytes > 0,
                "Number of bytes must be positive")

        dataPackMap.putIfAbsent(msisdn, 0L)
        val newDataSize = dataPackMap[msisdn]!! + bytes
        dataPackMap[msisdn] = newDataSize
        return newDataSize
    }

    /**
     * Release any reserved bucket of bytes for this subscriber.
     *
     * @param msisdn Phone number
     * @return released bucket size in bytes
     */
    fun releaseReservedBucket(msisdn: String): Long {

        val reserved = bucketReservedMap.remove(msisdn)
        if (reserved == null || reserved == 0L) {
            LOG.warn("Trying to release not existing reserved bucket for msisdn {}", msisdn)
            return 0
        }

        addDataBundleBytes(msisdn, reserved)

        return reserved
    }

    /**
     * Consume from subscriber's data bundle in bytes.
     * This is called when P-GW has used a data bucket.
     *
     * @param msisdn Phone number
     * @param usedBytes Consume a number of bytes
     * @return Number of bytes left in bundle
     */
    fun consumeDataBytes(msisdn: String, usedBytes: Long): Long {

        Preconditions.checkArgument(usedBytes > -1, "Non-positive value for bytes")

        if (!dataPackMap.containsKey(msisdn)) {
            LOG.warn("Used-Units for unknown msisdn : {}", msisdn)
            return 0
        }

        val existing = dataPackMap[msisdn] ?: 0

        val reserved = bucketReservedMap.remove(msisdn)
        if (reserved == null || reserved == 0L) {
            // If there was usedBytes but no reservation, this indicates an error.
            // usedBytes = 0 and reserved = 0 is normal in CCR-I
            if (usedBytes > 0) {
                LOG.warn("Used-Units without reservation")
            }
            return existing
        }

        /* We chose to deduct the amount directly from the total bucket when reserved.
         * The usedBytes can be more or less then this reserved amount.
         * So we have to pay back or deduct from the existing amount in the bucket
         * depending on how much was actually spent.
         *
         * One could choose to only deduct from the total bucket when the bytes was actually
         * spent. But then there will be an issue if multiple msisdns share the same bundle.
         */

        val consumed = usedBytes - reserved
        var newTotal = existing - consumed

        // P-GW is allowed to overconsume a small amount.
        if (newTotal < 0) {
            newTotal = 0
        }

        dataPackMap[msisdn] = newTotal
        return newTotal
    }

    /**
     * Reserve from subscriber's data bundle in bytes.
     * This is called when P-GW has requested a data bucket.
     *
     * @param msisdn Phone number
     * @param bytes Reserve a number of bytes
     * @return Number of bytes actually reserved
     */
    fun reserveDataBytes(msisdn: String, bytes: Long): Long {

        Preconditions.checkArgument(bytes > -1, "Non-positive value for bytes")

        if (bytes == 0L) {
            return 0
        }

        if (!dataPackMap.containsKey(msisdn)) {
            LOG.warn("Trying to reserve bucket for unknown msisdn {}", msisdn)
            return 0
        }

        if (bucketReservedMap.containsKey(msisdn)) {
            LOG.warn("Bucket already reserved for {}", msisdn)
            return 0
        }

        val existing = dataPackMap[msisdn] ?: 0L
        if (existing == 0L) {
            return 0
        }

        val consumed = Math.min(existing, bytes)
        bucketReservedMap[msisdn] = consumed
        dataPackMap[msisdn] = existing - consumed
        return consumed
    }

    init {
        if (loadSubscriberInfo) {
            loadSubscriberBalanceFromDatabaseToInMemoryStructure()
        }
    }

    private fun loadSubscriberBalanceFromDatabaseToInMemoryStructure() {
        LOG.info("Loading initial balance from storage to in-memory OcsState")
        val store: Storage = getResource()
        val subscribers = store.allSubscribers
        for (subscriber in subscribers) {
            val msisdn = subscriber.msisdn
            val noOfBytesLeft = subscriber.noOfBytesLeft
            LOG.info("{} - {}", msisdn, noOfBytesLeft)
            if (noOfBytesLeft > 0) {
                val newMsisdn = stripLeadingPlus(msisdn)
                addDataBundleBytes(newMsisdn, noOfBytesLeft)
            }
        }
    }

    companion object {
        fun stripLeadingPlus(str: String): String {
            return str.replaceFirst("^\\+".toRegex(), "")
        }
    }
}
