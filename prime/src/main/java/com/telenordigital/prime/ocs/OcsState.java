package com.telenordigital.prime.ocs;

import com.google.common.base.Preconditions;
import com.lmax.disruptor.EventHandler;
import com.telenordigital.prime.disruptor.PrimeEvent;
import com.telenordigital.prime.storage.entities.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class OcsState implements EventHandler<PrimeEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OcsState.class);

    private final Map<String, Long> dataPackMap = new HashMap<>();

    @Override
    public void onEvent(
            final PrimeEvent event,
            final long sequence,
            final boolean endOfBatch) {
        try {
            switch (event.getMessageType()) {
                case FETCH_DATA_BUCKET:
                    event.setBucketBytes(
                            consumeDataBytes(
                                    event.getMsisdn(),
                                    event.getBucketBytes()));
                    event.setBundleBytes(getDataBytes(event.getMsisdn()));
                    break;
                case RETURN_UNUSED_DATA_BUCKET:
                case TOPUP_DATA_BUNDLE_BALANCE:
                    event.setBundleBytes(addDataBytes(event.getMsisdn(), event.getBucketBytes()));
                    break;
                case GET_DATA_BUNDLE_BALANCE:
                    event.setBundleBytes(getDataBytes(event.getMsisdn()));
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
    public long getDataBytes(final String msisdn) {
        return dataPackMap.getOrDefault(msisdn, 0L);
    }

    /**
     * Add to subscriber's data bundle balance in bytes.
     * This is called when subscriber top ups or, PGw returns
     * unused data after subscriber disconnects data.
     *
     * @param msisdn Phone number
     * @param bytes Number of bytes we want to add
     * @return bytes data bundle balance in bytes
     */
    public long addDataBytes(final String msisdn, final long bytes) {
        checkNotNull(msisdn);
        Preconditions.checkArgument(bytes > 0,
                "No of bytes must be positive");

        dataPackMap.putIfAbsent(msisdn, 0L);
        long newDataSize = dataPackMap.get(msisdn) + bytes;
        dataPackMap.put(msisdn, newDataSize);
        return newDataSize;
    }

    /**
     * Consume from subscriber's data bundle in bytes.
     * This is called when PGw requests for data bucket.
     *
     * @param msisdn Phone number
     * @param bytes Consume a number of bytes
     * @return Number of bytes actually consumed
     */
    public long consumeDataBytes(final String msisdn, final long bytes) {
        checkNotNull(msisdn);
        Preconditions.checkArgument(bytes > 0, "Non-positive value for bytes");

        if (!dataPackMap.containsKey(msisdn)) {
            return 0;
        }

        long existing = dataPackMap.get(msisdn);
        if (existing == 0) {
            return 0;
        }

        final long consumed = Math.min(existing, bytes);
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
            addDataBytes(msisdn, subscriber.getNoOfBytesLeft());
        }
    }
}
