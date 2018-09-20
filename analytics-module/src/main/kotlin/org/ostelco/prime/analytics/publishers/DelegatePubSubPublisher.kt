package org.ostelco.prime.analytics.publishers

import com.google.api.core.ApiFuture
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.Publisher
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import io.grpc.ManagedChannelBuilder
import org.ostelco.prime.analytics.ConfigRegistry
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class DelegatePubSubPublisher(
        private val topicId: String,
        private val projectId: String = ConfigRegistry.config.projectId) : PubSubPublisher {

    private lateinit var publisher: Publisher

    override lateinit var singleThreadScheduledExecutor: ScheduledExecutorService

    override fun start() {

        singleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor()

        val topicName = ProjectTopicName.of(projectId, topicId)
        val strSocketAddress = System.getenv("PUBSUB_EMULATOR_HOST")
        publisher = if (!strSocketAddress.isNullOrEmpty()) {
            val channel = ManagedChannelBuilder.forTarget(strSocketAddress).usePlaintext().build()
            // Create a publisher instance with default settings bound to the topic
            val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
            Publisher.newBuilder(topicName)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(NoCredentialsProvider())
                    .build();
        } else {
            Publisher.newBuilder(topicName).build()
        }
    }

    override fun stop() {
        // When finished with the publisher, shutdown to free up resources.
        publisher.shutdown()
        singleThreadScheduledExecutor.shutdown()
    }

    override fun publishPubSubMessage(pubsubMessage: PubsubMessage): ApiFuture<String> = publisher.publish(pubsubMessage)
}