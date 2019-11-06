package org.ostelco.prime.paymentprocessor.publishers

import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import com.google.pubsub.v1.PubsubMessage
import com.stripe.model.Event
import org.ostelco.prime.paymentprocessor.ConfigRegistry
import java.time.Instant
import org.ostelco.common.publisherex.DelegatePubSubPublisher
import org.ostelco.common.publisherex.PubSubPublisher


object StripeEventPublisher :
        PubSubPublisher by DelegatePubSubPublisher(
                topicId = ConfigRegistry.config.stripeEventTopicId,
                projectId = ConfigRegistry.config.projectId) {

    fun publish(event: Event) {

        val message = PubsubMessage.newBuilder()
                .setPublishTime(Timestamp.newBuilder()
                        .setSeconds(Instant.now().epochSecond))
                .setData(event.byteString())
                .build()
        publishPubSubMessage(message)
    }

    /* Monkeypatching uber alles! */
    private fun Event.byteString(): ByteString = ByteString.copyFromUtf8(this.toJson())

}
