package org.ostelco.prime.paymentprocessor.subscribers

import arrow.core.Try
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.protobuf.ByteString
import com.stripe.model.Event
import com.stripe.net.ApiResource.GSON
import org.ostelco.prime.getLogger
import org.ostelco.prime.paymentprocessor.ConfigRegistry
import org.ostelco.prime.pubsub.PubSubSubscriber
import org.ostelco.prime.store.datastore.DatastoreExcludeFromIndex
import org.ostelco.prime.store.datastore.EntityStore


class StoreStripeEvent : PubSubSubscriber(
        subscription = ConfigRegistry.config.stripeEventStoreSubscriptionId,
        topic = ConfigRegistry.config.stripeEventTopicId,
        project = ConfigRegistry.config.projectId) {

    private val logger by getLogger()

    /* GCP datastore. */
    private val entityStore = EntityStore(StripeEvent::class,
            type = ConfigRegistry.config.stripeEventStoreType,
            namespace = ConfigRegistry.config.namespace)

    override fun handler(message: ByteString, consumer: AckReplyConsumer) =
            Try {
                GSON.fromJson(message.toStringUtf8(), Event::class.java)
            }.fold(
                    ifSuccess = { event ->
                        entityStore.add(StripeEvent(event.type,
                                event.account,
                                event.created,
                                message.toStringUtf8()))
                                .mapLeft {
                                    logger.error("Failed to store Stripe event {}: {}",
                                            event.id, it.message)
                                }
                        consumer.ack()
                    },
                    ifFailure = {
                        logger.error("Failed to decode Stripe event for logging and error reporting: {}",
                                it.message)
                        consumer.ack()
                    }
            )
}

data class StripeEvent(val type: String,
                       val account: String?,
                       val created: Long,
                       @DatastoreExcludeFromIndex
                       val json: String)
