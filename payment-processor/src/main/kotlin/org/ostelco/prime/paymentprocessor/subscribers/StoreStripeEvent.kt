package org.ostelco.prime.paymentprocessor.subscribers

import arrow.core.Either
import arrow.core.Try
import com.google.cloud.datastore.FullEntity
import com.google.cloud.datastore.Key
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.protobuf.ByteString
import com.stripe.model.Event
import com.stripe.net.ApiResource.GSON
import org.ostelco.prime.getLogger
import org.ostelco.prime.paymentprocessor.ConfigRegistry
import org.ostelco.prime.paymentprocessor.StripeStore
import org.ostelco.prime.pubsub.PubSubSubscriber


enum class StripeProperty(val text: String) {
    ID("id"),
    CREATED("created"),
    TYPE("type"),
    DATA("data"),
    KIND("stripe-events")
}

class StoreStripeEvent : PubSubSubscriber(
        subscription = ConfigRegistry.config.stripeEventStoreSubscriptionId,
        topic = ConfigRegistry.config.stripeEventTopicId,
        project = ConfigRegistry.config.projectId) {

    private val logger by getLogger()

    /* GCP datastore. */
    private val datastore = StripeStore.datastore
    private val keyFactory = StripeStore.keyFactory

    override fun handler(message: ByteString, consumer: AckReplyConsumer) =
            Try {
                GSON.fromJson(message.toStringUtf8(), Event::class.java)
            }.fold(
                    ifSuccess = { event ->
                        store(event, message.toStringUtf8())
                                .mapLeft {
                                    logger.error("Failed to store Stripe event {}: {}",
                                            event.id, it)
                                }
                        consumer.ack()
                    },
                    ifFailure = {
                        logger.error("Failed to decode Stripe event for logging and error reporting: {}",
                                it)
                        consumer.ack()
                    }
            )

    private fun store(event: Event, data: String): Either<Throwable, Key> =
            Try {
                datastore.add(FullEntity.newBuilder(keyFactory.newKey())
                        .set(StripeProperty.ID.text, event.id)
                        .set(StripeProperty.CREATED.text, event.created)
                        .set(StripeProperty.TYPE.text, event.type)
                        .set(StripeProperty.DATA.text, data)      /* Maybe store as a blob? */
                        .build()).getKey()
            }.toEither()
}
