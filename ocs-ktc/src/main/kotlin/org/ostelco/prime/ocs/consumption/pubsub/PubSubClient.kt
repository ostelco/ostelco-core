package org.ostelco.prime.ocs.consumption.pubsub

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.ApiException
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.api.gax.rpc.TransportChannelProvider
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.Subscriber
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import io.dropwizard.lifecycle.Managed
import io.grpc.ManagedChannelBuilder
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.prime.getLogger
import org.ostelco.prime.activation.Activation
import org.ostelco.prime.ocs.consumption.OcsAsyncRequestConsumer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class PubSubClient(
        private val ocsAsyncRequestConsumer: OcsAsyncRequestConsumer,
        private val projectId: String,
        private val activateTopicId: String,
        private val ccrSubscriptionId: String) : Managed, Activation {

    private val logger by getLogger()

    private var singleThreadScheduledExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var pubSubChannelProvider: TransportChannelProvider? = null

    private var activatePublisher: Publisher? = null
    private var ccrPublisherMaps: ConcurrentHashMap<String, Publisher> = ConcurrentHashMap()

    override fun start() {

        val strSocketAddress = System.getenv("PUBSUB_EMULATOR_HOST") ?: System.getProperty("PUBSUB_EMULATOR_HOST")
        if (!strSocketAddress.isNullOrBlank()) {
            val channel = ManagedChannelBuilder.forTarget(strSocketAddress).usePlaintext().build()
            // Create a publisher instance with default settings bound to the topic
            pubSubChannelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
        }

        // init publishers
        activatePublisher = setupPublisherToTopic(activateTopicId)

        // init subscriber
        setupPubSubSubscriber(subscriptionId = ccrSubscriptionId) { message, consumer ->
                val ccrInfo = CreditControlRequestInfo.parseFrom(message)
                ocsAsyncRequestConsumer.creditControlRequestEvent(ccrInfo) {
                    publish(messageId = ccrInfo.requestId,
                            byteString = it.toByteString(),
                            publisher = ccrPublisherMaps.getOrPut(ccrInfo.topicId) {
                                setupPublisherToTopic(ccrInfo.topicId)
                            })
                }
                consumer.ack()
        }
    }

    override fun stop() {
        activatePublisher?.shutdown()
        activatePublisher?.awaitTermination(1, TimeUnit.MINUTES)
    }

    override fun activate(msisdn: String) {
        val activateResponse = ActivateResponse.newBuilder()
                .setMsisdn(msisdn)
                .build()

        activatePublisher?.apply {
            publish(messageId = UUID.randomUUID().toString(),
                    byteString = activateResponse.toByteString(),
                    publisher = this)
        }
    }

    internal fun publish(messageId: String, byteString: ByteString, publisher: Publisher) {

        val base64String = Base64.getEncoder().encodeToString(byteString.toByteArray())
        val pubsubMessage = PubsubMessage.newBuilder()
                .setMessageId(messageId)
                .setData(ByteString.copyFromUtf8(base64String))
                .build()

        val future = publisher.publish(pubsubMessage)

        ApiFutures.addCallback(future, object : ApiFutureCallback<String> {

            override fun onFailure(throwable: Throwable) {
                if (throwable is ApiException) {
                    // details on the API exception
                    logger.warn("Pubsub messageId: $messageId\n" +
                            "Message : ${throwable.message}\n" +
                            "Status code: ${throwable.statusCode.code}\n" +
                            "Retrying: ${throwable.isRetryable}")
                }
                logger.error("Error sending CCR Request to PubSub")
            }

            override fun onSuccess(messageId: String) {
                // Once published, returns server-assigned message ids (unique within the topic)
                //logger.debug("Submitted message with request-id: {} successfully", messageId)
            }
        }, singleThreadScheduledExecutor)
    }

    internal fun setupPubSubSubscriber(subscriptionId: String, handler: (ByteString, AckReplyConsumer) -> Unit) {
        // init subscriber
        logger.info("Setting up Subscriber for subscription: {}", subscriptionId)
        val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)

        val receiver = MessageReceiver { message, consumer ->
            val base64String = message.data.toStringUtf8()
            handler(ByteString.copyFrom(Base64.getDecoder().decode(base64String)), consumer)
        }

        val subscriber: Subscriber?
        try {
            // Create a subscriber for "my-subscription-id" bound to the message receiver
            subscriber = pubSubChannelProvider
                    ?.let {channelProvider ->
                        Subscriber.newBuilder(subscriptionName, receiver)
                                .setChannelProvider(channelProvider)
                                .setCredentialsProvider(NoCredentialsProvider())
                                .build()
                    }
                    ?: Subscriber.newBuilder(subscriptionName, receiver)
                            .build()
            subscriber?.startAsync()?.awaitRunning()
        } finally {
            // TODO vihang: Stop this in Managed.stop()
            // stop receiving messages
            // subscriber?.stopAsync()
        }
    }

    internal fun setupPublisherToTopic(topicId: String): Publisher {
        logger.info("Setting up Publisher for topic: {}", topicId)
        val topicName = ProjectTopicName.of(projectId, topicId)
        return pubSubChannelProvider
                ?.let { channelProvider ->
                    Publisher.newBuilder(topicName)
                            .setChannelProvider(channelProvider)
                            .setCredentialsProvider(NoCredentialsProvider())
                            .build()
                }
                ?: Publisher.newBuilder(topicName).build()
    }
}