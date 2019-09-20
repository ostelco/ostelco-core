package org.ostelco.prime.analytics.publishers

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.rpc.ApiException
import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.DataTrafficInfo
import java.time.Instant

/**
 * This holds logic for publishing the data consumption information events to Google Cloud Pub/Sub.
 */
object DataConsumptionInfoPublisher :
        PubSubPublisher by DelegatePubSubPublisher(topicId = ConfigRegistry.config.dataTrafficTopicId) {

    private val logger by getLogger()
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
        val future = publishPubSubMessage(pubsubMessage)

        // add an asynchronous callback to handle success / failure
        ApiFutures.addCallback(future, object : ApiFutureCallback<String> {

            override fun onFailure(throwable: Throwable) {
                if (throwable is ApiException) {
                    // details on the API exception
                    logger.warn("Status code: {}", throwable.statusCode.code)
                    logger.warn("Retrying: {}", throwable.isRetryable)
                }
                logger.warn("Error publishing message for subscriptionAnalyticsId: {}", subscriptionAnalyticsId)
            }

            override fun onSuccess(messageId: String) {
                // Once published, returns server-assigned message ids (unique within the topic)
                logger.debug("Published message $messageId")
            }
        }, singleThreadScheduledExecutor)
    }

    private fun toJson(dataTrafficInfo: DataTrafficInfo): ByteString {
        return ByteString.copyFromUtf8(gson.toJson(dataTrafficInfo))
    }
}
