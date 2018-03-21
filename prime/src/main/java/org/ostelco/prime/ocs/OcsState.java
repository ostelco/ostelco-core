package org.ostelco.prime.ocs;

import com.google.common.base.Preconditions;
import com.lmax.disruptor.EventHandler;
import org.ostelco.prime.disruptor.PrimeEvent;
import org.ostelco.prime.storage.entities.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class OcsState implements EventHandler<PrimeEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OcsState.class);

    private final Map<String, Long> dataPackMap = new HashMap<>();
    private final Map<String, Long> bucketReservedMap = new HashMap<>();

    @Override
    public void onEvent(
            final PrimeEvent event,
            final long sequence,
            final boolean endOfBatch) {
        try {
            switch (event.getMessageType()) {
                case CREDIT_CONTROL_REQUEST:
                    consumeDataBytes(event.getMsisdn(), event.getUsedBucketBytes());
                    event.setReservedBucketBytes(
                            reserveDataBytes(
                                    event.getMsisdn(),
                                    event.getRequestedBucketBytes()));
                    event.setBundleBytes(getDataBundleBytes(event.getMsisdn()));
                    break;
                case TOPUP_DATA_BUNDLE_BALANCE:
                    event.setBundleBytes(addDataBundleBytes(event.getMsisdn(), event.getRequestedBucketBytes()));
                    break;
                case GET_DATA_BUNDLE_BALANCE:
                    event.setBundleBytes(getDataBundleBytes(event.getMsisdn()));
                    break;
                case RELEASE_RESERVED_BUCKET:
                    releaseReservedBucket(event.getMsisdn());
                    break;
                default:
            }
        } catch (Exception e) {
            LOG.warn("Exception handling prime event in OcsState", e);
        }
    }

    /**
     * Get subscriber's data bundle balance in bytes.
     *
     * @param msisdn Phone number
     * @return bytes Number of bytes available to this number
     * data bundle balance in bytes
     */
    public long getDataBundleBytes(final String msisdn) {
        return dataPackMap.getOrDefault(msisdn, 0L);
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
    public long addDataBundleBytes(final String msisdn, final long bytes) {
        checkNotNull(msisdn);
        Preconditions.checkArgument(bytes > 0,
                "No of bytes must be positive");

        dataPackMap.putIfAbsent(msisdn, 0L);
        long newDataSize = dataPackMap.get(msisdn) + bytes;
        dataPackMap.put(msisdn, newDataSize);
        return newDataSize;
    }

    /**
     * Release any reserved bucket of bytes for this subscriber.
     *
     * @param msisdn Phone number
     * @return released bucket size in bytes
     */
    public long releaseReservedBucket(final String msisdn) {
        checkNotNull(msisdn);

        final Long reserved = bucketReservedMap.remove(msisdn);
        if (reserved == null || reserved == 0) {
            LOG.warn("Trying to release not existing reserved bucket for msisdn {}", msisdn);
            return 0;
        }

        addDataBundleBytes(msisdn, reserved);

        return reserved;
    }

    /**
     * Consume from subscriber's data bundle in bytes.
     * This is called when P-GW has used a data bucket.
     *
     * @param msisdn Phone number
     * @param usedBytes Consume a number of bytes
     * @return Number of bytes left in bundle
     */
    public long consumeDataBytes(final String msisdn, final long usedBytes) {
        checkNotNull(msisdn);
        Preconditions.checkArgument(usedBytes > -1, "Non-positive value for bytes");

        if (!dataPackMap.containsKey(msisdn)) {
            LOG.warn("Used-Units for unknown msisdn : " + msisdn);
            return 0;
        }

        final long existing = dataPackMap.get(msisdn);

        final Long reserved = bucketReservedMap.remove(msisdn);
        if (reserved == null || reserved == 0) {
            // If there was usedBytes but no reservation, this indicates an error.
            // usedBytes = 0 and reserved = 0 is normal in CCR-I
            if (usedBytes > 0) {
                LOG.warn("Used-Units without reservation");
            }
            return existing;
        }

        /* We chose to deduct the amount directly from the total bucket when reserved.
         * The usedBytes can be more or less then this reserved amount.
         * So we have to pay back or deduct from the existing amount in the bucket
         * depending on how much was actually spent.
         *
         * One could choose to only deduct from the total bucket when the bytes was actually
         * spent. But then there will be an issue if multiple msisdns share the same bundle.
         */

        final long consumed = usedBytes - reserved;
        final long newTotal = existing - consumed;
        dataPackMap.put(msisdn, newTotal);
        return newTotal;
    }

    /**
     * Reserve from subscriber's data bundle in bytes.
     * This is called when P-GW has requested a data bucket.
     *
     * @param msisdn Phone number
     * @param bytes Reserve a number of bytes
     * @return Number of bytes actually reserved
     */
    public long reserveDataBytes(final String msisdn, final long bytes) {
        checkNotNull(msisdn);
        Preconditions.checkArgument(bytes > -1, "Non-positive value for bytes");

        if (!dataPackMap.containsKey(msisdn)) {
            LOG.warn("Trying to reserve bucket for unknown msisdn {}", msisdn);
            return 0;
        }

        if (bucketReservedMap.containsKey(msisdn)) {
            LOG.warn("Bucket already reserved for {}", msisdn);
            return 0;
        }

        long existing = dataPackMap.get(msisdn);
        if (existing == 0) {
            return 0;
        }

        final long consumed = Math.min(existing, bytes);
        bucketReservedMap.put(msisdn, consumed);
        dataPackMap.put(msisdn, existing - consumed);
        return consumed;
    }

    public static String stripLeadingPlus(final String str) {
        checkNotNull(str);
        return str.replaceFirst("^\\+", "");
    }

    public void injectSubscriberIntoOCS(final Subscriber subscriber) {
        LOG.info("{} - {}", subscriber.getMsisdn(), subscriber.getNoOfBytesLeft());
        if (subscriber.getNoOfBytesLeft() > 0) {
            final String msisdn = stripLeadingPlus(subscriber.getMsisdn());
            addDataBundleBytes(msisdn, subscriber.getNoOfBytesLeft());
        }
    }
}
