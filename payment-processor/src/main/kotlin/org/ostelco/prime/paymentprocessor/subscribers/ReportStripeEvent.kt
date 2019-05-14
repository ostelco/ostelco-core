package org.ostelco.prime.paymentprocessor.subscribers

import arrow.core.Try
import arrow.core.getOrElse
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.protobuf.ByteString
import com.stripe.model.Event
import com.stripe.net.ApiResource.GSON
import org.ostelco.prime.getLogger
import org.ostelco.prime.paymentprocessor.ConfigRegistry
import org.ostelco.prime.pubsub.PubSubSubscriber

class ReportStripeEvent : PubSubSubscriber(
        subscription = ConfigRegistry.config.stripeEventReportSubscriptionId,
        topic = ConfigRegistry.config.stripeEventTopicId,
        project = ConfigRegistry.config.projectId) {

    private val logger by getLogger()

    override fun handler(message: ByteString, consumer: AckReplyConsumer) =
            Try {
                GSON.fromJson(message.toStringUtf8(), Event::class.java)
            }.fold(
                    ifSuccess = { event ->
                        Try {
                            Reporter.report(event)
                        }.getOrElse {
                            logger.error("Attempt to log Stripe event {} failed with error message: {}",
                                    message.toStringUtf8(), it.message)
                        }
                        consumer.ack()
                    },
                    ifFailure = {
                        logger.error("Failed to decode Stripe event for logging and error reporting: {}",
                                it.message)
                        /* If unparsable JSON then this should not affect
                           upstream, as the message is invalid. */
                        consumer.ack()
                    }
            )
}