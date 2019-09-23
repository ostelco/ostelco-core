package org.ostelco.prime.analytics.publishers

import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.model.SimProfileStatus
import org.ostelco.prime.model.SubscriptionStatusUpdate
import java.time.Instant

/**
 * This holds logic for sending SIM profile (=== subscription) status update events to Cloud Pub/Sub.
 */
object SubscriptionStatusUpdatePublisher :
        PubSubPublisher by DelegatePubSubPublisher(topicId = ConfigRegistry.config.subscriptionStatusUpdateTopicId) {

    private val gson = Gson()

    fun publish(subscriptionAnalyticsId: String, status: SimProfileStatus) {
        val subscriptionStatusUpdate = SubscriptionStatusUpdate(
                subscriptionAnalyticsId = subscriptionAnalyticsId,
                status = status,
                timestamp = Instant.now().toEpochMilli())

        publishPubSubMessage(PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(gson.toJson(subscriptionStatusUpdate)))
                .build()
        )
    }
}
