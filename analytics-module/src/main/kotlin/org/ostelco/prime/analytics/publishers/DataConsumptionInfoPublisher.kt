package org.ostelco.prime.analytics.publishers

import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.model.DataTrafficInfo
import java.time.Instant

/**
 * This holds logic for publishing the data consumption information events to Google Cloud Pub/Sub.
 */
object DataConsumptionInfoPublisher :
        PubSubPublisher by DelegatePubSubPublisher(topicId = ConfigRegistry.config.dataTrafficTopicId) {

    private val gson = Gson()

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

        val dataTrafficInfo = DataTrafficInfo(
                timestamp = Instant.now().toEpochMilli(),
                subscriptionAnalyticsId = subscriptionAnalyticsId,
                usedBucketBytes = usedBucketBytes,
                bundleBytes = bundleBytes,
                apn = apn,
                mccMnc = mccMnc
        )

        val pubsubMessage = PubsubMessage.newBuilder()
                .setData(toJson(dataTrafficInfo))
                .build()

        // schedule a message to be published, messages are automatically batched
        publishPubSubMessage(pubsubMessage)
    }

    private fun toJson(dataTrafficInfo: DataTrafficInfo): ByteString {
        return ByteString.copyFromUtf8(gson.toJson(dataTrafficInfo))
    }
}
