package org.ostelco.prime.analytics.publishers

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.rpc.ApiException
import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.google.protobuf.util.Timestamps
import com.google.pubsub.v1.PubsubMessage
import org.ostelco.analytics.api.DataTrafficInfo
import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.getLogger
import java.time.Instant

/**
 * This class publishes the data consumption information events to the Google Cloud Pub/Sub.
 */
object DataConsumptionInfoPublisher :
        PubSubPublisher by DelegatePubSubPublisher(topicId = ConfigRegistry.config.dataTrafficTopicId) {

    private val logger by getLogger()
    private val gson = Gson()

    fun publish(subscriptionAnalyticsId: String, usedBucketBytes: Long, bundleBytes: Long, apn: String?, mccMnc: String?) {

        if (usedBucketBytes == 0L) {
            return
        }

        val now = Instant.now().toEpochMilli()

        val dataTrafficInfo = DataTrafficInfo.newBuilder()
                .setSubscriptionAnalyticsId(subscriptionAnalyticsId)
                .setUsedBucketBytes(usedBucketBytes)
                .setBundleBytes(bundleBytes)
                .setTimestamp(Timestamps.fromMillis(now))
                .setApn(apn)
                .setMccMnc(mccMnc)
                .build()

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
