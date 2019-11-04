package org.ostelco.at.common

import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectSubscriptionName
import com.stripe.model.Event
import com.stripe.model.Invoice
import com.stripe.model.Subscription
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

object StripeEventListener {

    private val logger by getLogger()

    val topic = "stripe-event"

    fun waitForSubscriptionPaymentToSucceed(topic: String = StripeEventListener.topic,
                                            subscription: String,
                                            customerId: String,
                                            timeout: Long = 10000L): Boolean =
            waitForStripeEvent(
                    topic = topic,
                    subscription = subscription,
                    eventType = "invoice.payment_succeeded",
                    timeout = timeout,
                    cmp = {
                        if (it.type != "invoice.payment_succeeded")
                            false
                        else {
                            val event = it.data.`object` as Invoice
                            (event.customer == customerId)
                                    .also {
                                        if (it)
                                            logger.info("Received 'invoice.payment_succeeded' event for customer $customerId")
                                    }
                        }
                    })

    fun waitForFailedSubscriptionRenewal(topic: String = StripeEventListener.topic,
                                         subscription: String,
                                         customerId: String,
                                         timeout: Long = 10000L): Boolean =
            waitForStripeEvent(
                    topic = topic,
                    subscription = subscription,
                    eventType = "customer.subscription.updated",
                    timeout = timeout,
                    cmp = {
                        if (it.type != "customer.subscription.updated")
                            false
                        else {
                            val event = it.data.`object` as Subscription
                            (event.status == "past_due" && event.customer == customerId)
                                    .also {
                                        if (it)
                                            logger.info("Received 'customer.subscription.updated' from Stripe for customer $customerId with status 'past_due'")
                                    }
                        }
                    })

    private fun waitForStripeEvent(topic: String, subscription: String, eventType: String, timeout: Long, cmp: (Event) -> Boolean): Boolean {
        var status = false
        val subscriber = PubSubSubscriber(
                topic = topic,
                subscription = subscription,
                cmp = cmp
        )

        try {
            runBlocking {
                status = withTimeoutOrNull(timeout) {
                    subscriber.start()
                    while (!subscriber.eventSeen()) {
                        delay(200L)
                    }
                    "Done"
                } != null
                if (!status)
                    logger.error("Reached timeout limit $timeout while waiting for event $eventType")
            }
        } finally {
            subscriber.stop()
        }

        return status
    }
}

/**
 * The pubsub setup is for use with emulator only.
 */
class PubSubSubscriber(val topic: String,
                       val subscription: String,
                       val cmp: (Event) -> Boolean) {

    private val logger by getLogger()
    private val gson = Gson()

    private val hostport: String = System.getenv("PUBSUB_EMULATOR_HOST")
    private val project: String = System.getenv("GCP_PROJECT_ID")

    private val subscriptionName = ProjectSubscriptionName.of(project,
            subscription)
    private val channel = ManagedChannelBuilder.forTarget(hostport)
            .usePlaintext()
            .build()
    private val channelProvider = FixedTransportChannelProvider
            .create(GrpcTransportChannel.create(channel))
    private val receiver = MessageReceiver { message, consumer ->
        handler(message.data, consumer)
    }

    /* For managment. */
    private var subscriber = Subscriber.newBuilder(subscriptionName, receiver)
            .setChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .build()

    fun start() {
        logger.info("ACCEPTANCE TEST PUBSUB: Enabling subscription ${subscription} for project ${project} and topic ${topic}")

        try {
            subscriber.startAsync().awaitRunning()
        } catch (e: Exception) {
            logger.error("ACCEPTANCE TEST PUBSUB: Failed to connect to service: {}",
                    e.message)
        }
    }

    fun stop() {
        try {
            subscriber.stopAsync()
        } catch (e: Exception) {
            logger.error("ACCEPTANCE TEST PUBSUB: Error disconnecting to service: {}",
                    e.message)
        }
    }

    private fun handler(message: ByteString, consumer: AckReplyConsumer) {
        try {
            gson.fromJson(message.toStringUtf8(), Event::class.java)
                    .let {
                        if (cmp(it)) eventSeen = true
                    }
        } catch (e: Exception) {
            logger.error("ACCEPTANCE TEST PUBSUB: Failed to decode JSON Stripe event for 'recurring payment' processing: {}",
                    e.message)
            eventSeen = false
        }
        consumer.ack()
    }

    /* Flag. */
    private var eventSeen = false

    /* True on match on expected event. */
    fun eventSeen() = eventSeen
}
