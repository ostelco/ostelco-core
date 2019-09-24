package org.ostelco.prime.analytics.publishers

import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.model.SimProvisioning
import java.time.Instant

/**
 * This holds logic for sending SIM provisioning events to Cloud Pub/Sub.
 */
object SimProvisioningPublisher :
        PubSubPublisher by DelegatePubSubPublisher(topicId = ConfigRegistry.config.simProvisioningTopicId) {

    private val gson = Gson()

    fun publish(subscriptionAnalyticsId: String, customerAnalyticsId: String, regionCode: String) {
        val simProvisioning = SimProvisioning(
                subscriptionAnalyticsId = subscriptionAnalyticsId,
                customerAnalyticsId = customerAnalyticsId,
                regionCode = regionCode,
                timestamp = Instant.now().toEpochMilli())

        publishPubSubMessage(
                PubsubMessage.newBuilder()
                        .setData(ByteString.copyFromUtf8(gson.toJson(simProvisioning)))
                        .build()
        )
    }
}
