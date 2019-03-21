package org.ostelco.prime.paymentprocessor.subscribers

import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.protobuf.ByteString
import org.ostelco.prime.getLogger
import org.ostelco.prime.paymentprocessor.ConfigRegistry
import org.ostelco.prime.pubsub.PubSubSubscriber


class StoreStripeEvent : PubSubSubscriber(
        subscription = ConfigRegistry.config.stripeEventStoreSubscriptionId,
        topic = ConfigRegistry.config.stripeEventTopicId,
        project = ConfigRegistry.config.projectId) {

    private val logger by getLogger()

    override fun handler(message: ByteString, consumer: AckReplyConsumer) {
//        logger.info(">>>")
//        logger.info(">>> message: ${message.toStringUtf8()}")
//        logger.info(">>>")

        consumer.ack()
    }
}