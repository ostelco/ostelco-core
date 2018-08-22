package org.ostelco.prime.ocs

import com.google.common.base.Preconditions
import com.lmax.disruptor.EventHandler
import org.ostelco.prime.disruptor.EventMessageType.ADD_MSISDN_TO_BUNDLE_MAPPING
import org.ostelco.prime.disruptor.EventMessageType.CREDIT_CONTROL_REQUEST
import org.ostelco.prime.disruptor.EventMessageType.RELEASE_RESERVED_BUCKET
import org.ostelco.prime.disruptor.EventMessageType.REMOVE_MSISDN_TO_BUNDLE_MAPPING
import org.ostelco.prime.disruptor.EventMessageType.TOPUP_DATA_BUNDLE_BALANCE
import org.ostelco.prime.disruptor.EventMessageType.UPDATE_BUNDLE
import org.ostelco.prime.disruptor.OcsEvent
import org.ostelco.prime.logger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource
import java.util.*

/**
 * For unit testing, loadSubscriberInfo = false
 */
class OcsState(val loadSubscriberInfo:Boolean = true) : EventHandler<OcsEvent> {

    private val logger by logger()

    // this is public for prime:integration tests
    val msisdnToBundleIdMap = HashMap<String, String>()
    val bundleIdToMsisdnMap = HashMap<String, MutableSet<String>>()

    private val bundleBalanceMap = HashMap<String, Long>()
    private val bucketReservedMap = HashMap<String, Long>()

    override fun onEvent(
            event: OcsEvent,
            sequence: Long,
            endOfBatch: Boolean) {
        try {

            when (event.messageType) {
                CREDIT_CONTROL_REQUEST -> {
                    val msisdn = event.msisdn
                    if (msisdn == null) {
                        logger.error("Received null as msisdn")
                        return
                    }
                    consumeDataBytes(msisdn, event.usedBucketBytes)
                    event.reservedBucketBytes = reserveDataBytes(
                            msisdn,
                            event.requestedBucketBytes)
                    event.bundleId = msisdnToBundleIdMap[msisdn]
                    event.bundleBytes = bundleBalanceMap[event.bundleId] ?: 0
                }
                TOPUP_DATA_BUNDLE_BALANCE -> {
                    val bundleId = event.bundleId
                    if (bundleId == null) {
                        logger.error("Received null as bundleId")
                        return
                    }
                    event.bundleBytes = addDataBundleBytes(bundleId, event.requestedBucketBytes)
                    event.msisdnToppedUp = bundleIdToMsisdnMap[bundleId]?.toList()
                }
                RELEASE_RESERVED_BUCKET -> {
                    val msisdn = event.msisdn
                    if (msisdn == null) {
                        logger.error("Received null as msisdn")
                        return
                    }
                    releaseReservedBucket(msisdn = msisdn)
                    event.bundleId = msisdnToBundleIdMap[msisdn]
                    event.bundleBytes = bundleBalanceMap[event.bundleId] ?: 0
                }
                UPDATE_BUNDLE -> {
                    val bundleId = event.bundleId
                    if (bundleId == null) {
                        logger.error("Received null as bundleId")
                        return
                    }
                    bundleBalanceMap[bundleId] = event.bundleBytes
                }
                ADD_MSISDN_TO_BUNDLE_MAPPING -> {
                    val msisdn = event.msisdn
                    if (msisdn == null) {
                        logger.error("Received null as msisdn")
                        return
                    }
                    val bundleId = event.bundleId
                    if (bundleId == null) {
                        logger.error("Received null as bundleId")
                        return
                    }
                    msisdnToBundleIdMap[msisdn] = bundleId
                    bundleIdToMsisdnMap.putIfAbsent(bundleId, mutableSetOf())
                    bundleIdToMsisdnMap[bundleId]?.add(msisdn)
                }
                REMOVE_MSISDN_TO_BUNDLE_MAPPING -> {
                    val msisdn = event.msisdn
                    if (msisdn == null) {
                        logger.error("Received null as msisdn")
                        return
                    }
                    val bundleId = event.bundleId
                    if (bundleId == null) {
                        logger.error("Received null as bundleId")
                        return
                    }
                    releaseReservedBucket(msisdn = msisdn)
                    event.bundleBytes = bundleBalanceMap[bundleId] ?: 0
                    msisdnToBundleIdMap.remove(msisdn)
                    bundleIdToMsisdnMap[bundleId]?.remove(msisdn)
                }
            }
        } catch (e: Exception) {
            logger.warn("Exception handling prime event in OcsState", e)
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
        val bundleId = msisdnToBundleIdMap[msisdn] ?: return 0
        return bundleBalanceMap[bundleId] ?: 0
    }

    /**
     * Add to subscriber's data bundle balance in bytes.
     * This is called when subscriber top ups. or, P-GW returns
     * unused data after subscriber disconnects data.
     *
     * @param bundleId Bundle ID
     * @param bytes Number of bytes we want to add
     * @return bytes data bundle balance in bytes
     */
    private fun addDataBundleBytes(bundleId: String, bytes: Long): Long {

        Preconditions.checkArgument(bytes > 0,
                "Number of bytes must be positive")

        bundleBalanceMap.putIfAbsent(bundleId, 0L)
        val newDataSize = bundleBalanceMap[bundleId]!! + bytes
        bundleBalanceMap[bundleId] = newDataSize
        return newDataSize
    }

    /**
     * Add to subscriber's data bundle balance in bytes.
     * This is called when subscriber top ups. or, P-GW returns
     * unused data after subscriber disconnects data.
     *
     * @param msisdn Phone number
     * @param bytes Number of bytes we want to add
     * @return bytes data bundle balance in bytes
     */
    fun addDataBundleBytesForMsisdn(msisdn: String, bytes: Long): Long {

        val bundleId = msisdnToBundleIdMap[msisdn] ?: return 0
        return addDataBundleBytes(bundleId, bytes)
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
            logger.warn("Trying to release not existing reserved bucket for msisdn {}", msisdn)
            return 0
        }

        addDataBundleBytesForMsisdn(msisdn, reserved)

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

        val bundleId = msisdnToBundleIdMap[msisdn]

        if (bundleId == null) {
            logger.warn("Used-Units for unknown msisdn : {}", msisdn)
            return 0
        }


        if (!bundleBalanceMap.containsKey(bundleId)) {
            logger.warn("Used-Units for unknown bundle : {}", bundleId)
            return 0
        }

        val existing = bundleBalanceMap[bundleId] ?: 0

        val reserved = bucketReservedMap.remove(msisdn)
        if (reserved == null || reserved == 0L) {
            // If there was usedBytes but no reservation, this indicates an error.
            // usedBytes = 0 and reserved = 0 is normal in CCR-I
            if (usedBytes > 0) {
                logger.warn("Used-Units without reservation")
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

        bundleBalanceMap[bundleId] = newTotal
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

        val bundleId = msisdnToBundleIdMap[msisdn]
        if (bundleId == null) {
            logger.warn("Trying to reserve bucket for unknown msisdn {}", msisdn)
            return 0
        }

        if (bucketReservedMap.containsKey(msisdn)) {
            logger.warn("Bucket already reserved for {}", msisdn)
            return 0
        }

        val existing = bundleBalanceMap[bundleId] ?: 0L
        if (existing == 0L) {
            return 0
        }

        val consumed = Math.min(existing, bytes)
        bucketReservedMap[msisdn] = consumed
        bundleBalanceMap[bundleId] = existing - consumed
        return consumed
    }

    init {
        if (loadSubscriberInfo) {
            loadDatabaseToInMemoryStructure()
        }
    }

    private fun loadDatabaseToInMemoryStructure() {

        logger.info("Loading initial balance from storage to in-memory OcsState")
        val store: AdminDataSource = getResource()

        msisdnToBundleIdMap.putAll(
                store.getMsisdnToBundleMap()
                        .map { it.key.msisdn to it.value.id })

        for ((msisdn, bundleId) in msisdnToBundleIdMap) {
            bundleIdToMsisdnMap.putIfAbsent(bundleId, mutableSetOf())
            bundleIdToMsisdnMap[bundleId]?.add(msisdn)
        }

        bundleBalanceMap.putAll(
                store.getAllBundles()
                        .filter { it.balance > 0 }
                        .map { it.id to it.balance })

        for ((msisdn, bundleId) in msisdnToBundleIdMap) {
            logger.info("msisdn :: bundleId - {} :: {}", msisdn, bundleId)
        }

        for ((bundleId, noOfBytesLeft) in bundleBalanceMap) {
            logger.info("bundleId :: noOfBytesLeft - {} :: {}", bundleId, noOfBytesLeft)
        }
    }
}
