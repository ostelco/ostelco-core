package org.ostelco.prime.paymentprocessor.publishers

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.rpc.ApiException
import com.google.common.util.concurrent.MoreExecutors
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import com.google.pubsub.v1.PubsubMessage
import com.stripe.model.Event
import org.ostelco.prime.getLogger
import org.ostelco.prime.paymentprocessor.ConfigRegistry
import org.ostelco.prime.pubsub.DelegatePubSubPublisher
import org.ostelco.prime.pubsub.PubSubPublisher
import java.time.Instant


object StripeEventPublisher :
        PubSubPublisher by DelegatePubSubPublisher(topicId = ConfigRegistry.config.stripeEventTopicId,
                projectId = ConfigRegistry.config.projectId) {

    private val logger by getLogger()

    fun publish(event: Event) {

        val message = PubsubMessage.newBuilder()
                .setPublishTime(Timestamp.newBuilder()
                        .setSeconds(Instant.now().epochSecond))
                .setData(event.byteString())
                .build()
        val future = publishPubSubMessage(message)

        ApiFutures.addCallback(future, object : ApiFutureCallback<String> {

            override fun onFailure(throwable: Throwable) {
                if (throwable is ApiException) {
                    logger.warn("Error publishing Stripe event: {} (status code: {}, retrying: {})",
                            event.id, throwable.statusCode.code, throwable.isRetryable)
                } else {
                    logger.warn("Error publishing Stripe event: {}", event.id)
                }
            }

            /* Once published, returns server-assigned message ids (unique
               within the topic) */
            override fun onSuccess(messageId: String) {
                logger.debug("Published Stripe event {} as message {}", event.id,
                        messageId)
            }
        }, MoreExecutors.directExecutor())
    }

    /* Monkeypatching uber alles! */
    private fun Event.byteString(): ByteString = ByteString.copyFromUtf8(this.toJson())
}