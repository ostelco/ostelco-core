package org.ostelco.prime.analytics.publishers

import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.analytics.events.DataConsumptionEvent

/**
 * This holds logic for publishing the data consumption information events to Google Cloud Pub/Sub.
 */
object DataConsumptionInfoPublisher :
        PubSubPublisher by DelegatePubSubPublisher(topicId = ConfigRegistry.config.dataTrafficTopicId) {

    /**
     * Publishes a new data consumption record to Cloud Pubsub
     *
     * @param subscriptionAnalyticsId UUID for the subscription consuming data (equivalent to MSISDN)
     * @param usedBucketBytes bytes bucket that was used prior to this event being sent
     * @param bundleBytes bytes left in the current bundle
     * @param apn access point name
     * @param mccMnc country/operator code pair
     */
    fun publish(subscriptionAnalyticsId: String, usedBucketBytes: Long, bundleBytes: Long, apn: String?, mccMnc: String?) {

        if (usedBucketBytes == 0L) {
            return
        }

        // schedule a message to be published, messages are automatically batched
        publishEvent(DataConsumptionEvent(
                subscriptionAnalyticsId = subscriptionAnalyticsId,
                usedBucketBytes = usedBucketBytes,
                bundleBytes = bundleBytes,
                apn = apn,
                mccMnc = mccMnc
        ))
    }
}
