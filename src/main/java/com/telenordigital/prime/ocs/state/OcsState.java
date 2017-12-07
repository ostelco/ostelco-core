package com.telenordigital.prime.ocs.state;

import com.google.common.base.Preconditions;
import com.lmax.disruptor.EventHandler;
import com.telenordigital.prime.disruptor.PrimeEvent;
import com.telenordigital.prime.events.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vihang Patil <vihang.patil@telenordigital.com>
 */
public class OcsState implements EventHandler<PrimeEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OcsState.class);

    private final Map<String, Long> dataPackMap = new HashMap<>();

    @Override
    public void onEvent(PrimeEvent event, long sequence, boolean endOfBatch) {
        try {
            switch (event.getMessageType()) {
                case FETCH_DATA_BUCKET:
                    event.setBucketBytes(consumeDataBytes(event.getMsisdn(), event.getBucketBytes()));
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
     * @param msisdn
     * @return bytes
     * data bundle balance in bytes
     */
    long getDataBytes(final String msisdn) {
        return dataPackMap.getOrDefault(msisdn, 0L);
    }

    /**
     * Add to subscriber's data bundle balance in bytes.
     * This is called when subscriber top ups or, PGw returns unused data after subscriber disconnects data.
     *
     * @param msisdn
     * @param bytes
     * @return bytes
     * data bundle balance in bytes
     */
    public long addDataBytes(final String msisdn, long bytes) {
        Preconditions.checkNotNull(msisdn);
        Preconditions.checkArgument(bytes > 0, "Non-positive value for bytes");

        dataPackMap.putIfAbsent(msisdn, 0L);
        long newDataSize = dataPackMap.get(msisdn) + bytes;
        dataPackMap.put(msisdn, newDataSize);
        return newDataSize;
    }

    /**
     * Consume from subscriber's data bundle in bytes.
     * This is called when PGw requests for data bucket.
     *
     * @param msisdn
     * @param bytes
     * @return
     */
    long consumeDataBytes(final String msisdn, final long bytes) {
        Preconditions.checkNotNull(msisdn);
        Preconditions.checkArgument(bytes > 0, "Non-positive value for bytes");

        if(!dataPackMap.containsKey(msisdn)) {
            return 0;
        }

        long existing = dataPackMap.get(msisdn);
        if(existing == 0) {
            return 0;
        }
        long consumed = Math.min(existing, bytes);
        dataPackMap.put(msisdn, existing - consumed);
        return consumed;
    }


    public void injectSubscriberIntoOCS(final Subscriber subscriber) {
        LOG.info("{} - {}", subscriber.getMsisdn(), subscriber.getNoOfBytesLeft());
        if (subscriber.getNoOfBytesLeft() > 0) {
            String msisdn = subscriber.getMsisdn();
            // XXX Use string rewriting methods instead.
            // XXX removing '+'
            if (msisdn.charAt(0) == '+') {
                msisdn = msisdn.substring(1);
            }
            addDataBytes(msisdn, subscriber.getNoOfBytesLeft());
        }
    }
}
