package org.ostelco.prime.analytics.publishers

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.ApiException
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.Publisher
import com.google.common.util.concurrent.MoreExecutors
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import io.grpc.ManagedChannelBuilder
import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.analytics.events.Event
import org.ostelco.prime.getLogger
import java.util.concurrent.TimeUnit

class DelegatePubSubPublisher(
        private val topicId: String,
        private val projectId: String = ConfigRegistry.config.projectId) : PubSubPublisher {

    private lateinit var publisher: Publisher
    private val logger by getLogger()

    override fun start() {

        val topicName = ProjectTopicName.of(projectId, topicId)
        val strSocketAddress = System.getenv("PUBSUB_EMULATOR_HOST")
        publisher = if (!strSocketAddress.isNullOrEmpty()) {
            val channel = ManagedChannelBuilder.forTarget(strSocketAddress).usePlaintext().build()
            // Create a publisher instance with default settings bound to the topic
            val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
            Publisher.newBuilder(topicName)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(NoCredentialsProvider())
                    .build()
        } else {
            Publisher.newBuilder(topicName).build()
        }
    }

    override fun stop() {
        // When finished with the publisher, shutdown to free up resources.
        publisher.shutdown()
        publisher.awaitTermination(1, TimeUnit.MINUTES)
    }

    override fun publishPubSubMessage(pubsubMessage: PubsubMessage) {
        val future = publisher.publish(pubsubMessage)

        // add an asynchronous callback to handle success / failure
        ApiFutures.addCallback(future, object : ApiFutureCallback<String> {

            override fun onFailure(throwable: Throwable) {
                if (throwable is ApiException) {
                    // details on the API exception
                    logger.warn("Error publishing message to Pubsub topic: $topicId\n" +
                            "Message: ${throwable.message}\n" +
                            "Status code: ${throwable.statusCode.code}\n" +
                            "Retrying: ${throwable.isRetryable}")
                } else {
                    logger.warn("Error publishing message to Pubsub topic: $topicId")
                }
            }

            override fun onSuccess(messageId: String) {
                // Once published, returns server-assigned message ids (unique within the topic)
                logger.debug("Published message $messageId to topic $topicId")
            }
        }, MoreExecutors.directExecutor())
    }

    override fun publishEvent(event: Event) {
        val message = PubsubMessage.newBuilder()
                .setData(event.toJsonByteString())
                .build()
        publishPubSubMessage(message)
    }
}
