package org.ostelco.prime.pubsub

import arrow.core.Try
import arrow.core.getOrElse
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.*
import com.google.protobuf.ByteString
import com.google.pubsub.v1.*
import io.dropwizard.lifecycle.Managed
import io.grpc.ManagedChannelBuilder
import org.ostelco.prime.getLogger

/**
 * Helper for subscribing to pubsub events.
 *
 * Assumes that referenced subscriptions exits.
 *
 * As the Java library don't uses the PUBSUB_EMULATOR_HOST environment variable
 * when setting up the connection to the pubsub server, an excplicit connection
 * needs to be created if this variable is set.
 *
 * Ref.: Section "Accessing environment variables"
 *       https://cloud.google.com/pubsub/docs/emulator#pubsub-emulator-java
 */
abstract class PubSubSubscriber(
        private val subscription: String,
        private val topic: String,
        private val project: String) : Managed {

    private val logger by getLogger()

    /* Is null if not used with emulator. */
    val hostport: String? = System.getenv("PUBSUB_EMULATOR_HOST")

    val subscriptionName = ProjectSubscriptionName.of(project,
            subscription)

    val receiver = MessageReceiver { message, consumer ->
        handler(message.data, consumer)
    }

    /* For managment of subscription. */
    var subscriber: Subscriber? = null

    override fun start() {
        logger.info("PUBSUB: Enabling subscription ${subscription} for project ${project} and topic ${topic}")

        subscriber = Try {
            (if (!hostport.isNullOrEmpty()) {
                val channel = ManagedChannelBuilder.forTarget(hostport)
                        .usePlaintext()
                        .build()
                val channelProvider = FixedTransportChannelProvider
                        .create(GrpcTransportChannel.create(channel))
                Subscriber.newBuilder(subscriptionName, receiver)
                        .setChannelProvider(channelProvider)
                        .setCredentialsProvider(NoCredentialsProvider.create())
                        .build()
            } else {
                Subscriber.newBuilder(subscriptionName, receiver)
                        .build()
            }).also { it.startAsync().awaitRunning() }
        }.getOrElse {
            logger.error("PUBSUB: Failed to connect to service: {}",
                    it)
            null
        }
    }

    override fun stop() {
        Try {
            subscriber?.stopAsync()
        }.getOrElse {
            logger.error("PUBSUB: Error disconnecting to service: {}",
                    it)
        }
    }

    /**
     * Handler for message fetched from a PUBSUB "subscription".
     * @param message  The message itself
     * @param consumer  Ack handler
     */
    abstract fun handler(message: ByteString, consumer: AckReplyConsumer)
}
