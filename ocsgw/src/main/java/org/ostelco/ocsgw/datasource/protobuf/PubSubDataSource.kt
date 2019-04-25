package org.ostelco.ocsgw.datasource.protobuf

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
import io.grpc.ManagedChannelBuilder
import org.ostelco.diameter.CreditControlContext
import org.ostelco.diameter.getLogger
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocsgw.datasource.DataSource
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService


class PubSubDataSource(
        private val protobufDataSource: ProtobufDataSource,
        projectId: String,
        ccrTopicId: String,
        private val ccaTopicId: String,
        ccaSubscriptionId: String,
        activateSubscriptionId: String) : DataSource {

    private val logger by getLogger()

    private var singleThreadScheduledExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var pubSubChannelProvider: TransportChannelProvider? = null
    private var publisher: Publisher

    init {

        val strSocketAddress = System.getenv("PUBSUB_EMULATOR_HOST")
        if (!strSocketAddress.isNullOrEmpty()) {
            val channel = ManagedChannelBuilder.forTarget(strSocketAddress).usePlaintext().build()
            // Create a publisher instance with default settings bound to the topic
            pubSubChannelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
        }

        // init publisher
        logger.info("Setting up Publisher for topic: {}", ccrTopicId)
        publisher = setupPublisherToTopic(projectId, ccrTopicId)

        // Instantiate an asynchronous message receiver
        setupPubSubSubscriber(projectId, ccaSubscriptionId) { message, consumer ->
            // handle incoming message, then ack/nack the received message
            val ccaInfo = CreditControlAnswerInfo.parseFrom(message)
            logger.info("[<<] CreditControlAnswer for {}", ccaInfo.msisdn)
            protobufDataSource.handleCcrAnswer(ccaInfo)
            consumer.ack()
        }

        setupPubSubSubscriber(projectId, activateSubscriptionId) { message, consumer ->
            // handle incoming message, then ack/nack the received message
            protobufDataSource.handleActivateResponse(
                    ActivateResponse.parseFrom(message))
            consumer.ack()
        }
    }

    override fun init() {

    }

    override fun handleRequest(context: CreditControlContext) {

        logger.info("[>>] creditControlRequest for {}", context.creditControlRequest.msisdn)

        val creditControlRequestInfo = protobufDataSource.handleRequest(context, ccaTopicId)

        if (creditControlRequestInfo != null) {
            val base64String = Base64.getEncoder().encodeToString(
                    creditControlRequestInfo.toByteArray())
            logger.debug("[>>] base64String: {}", base64String)
            val byteString = ByteString.copyFromUtf8(base64String)

            if (!byteString.isValidUtf8) {
                logger.warn("Could not convert creditControlRequestInfo to UTF-8")
                return
            }
            val pubsubMessage = PubsubMessage.newBuilder()
                    .setMessageId(creditControlRequestInfo.requestId)
                    .setData(byteString)
                    .build()

            //schedule a message to be published, messages are automatically batched
            val future = publisher.publish(pubsubMessage)

            // add an asynchronous callback to handle success / failure
            ApiFutures.addCallback(future, object : ApiFutureCallback<String> {

                override fun onFailure(throwable: Throwable) {
                    if (throwable is ApiException) {
                        // details on the API exception
                        logger.warn("Status code: {}", throwable.statusCode.code)
                        logger.warn("Retrying: {}", throwable.isRetryable)
                    }
                    logger.warn("Error sending CCR Request to PubSub")
                }

                override fun onSuccess(messageId: String) {
                    // Once published, returns server-assigned message ids (unique within the topic)
                    logger.debug("Submitted message with request-id: {} successfully", messageId)
                }
            }, singleThreadScheduledExecutor)
        }
    }

    override fun isBlocked(msisdn: String): Boolean = protobufDataSource.isBlocked(msisdn)

    private fun setupPublisherToTopic(projectId: String, topicId: String): Publisher {
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

    private fun setupPubSubSubscriber(projectId: String, subscriptionId: String, handler: (ByteString, AckReplyConsumer) -> Unit) {

        // init subscriber
        logger.info("Setting up Subscriber for subscription: {}", subscriptionId)
        val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)

        val receiver = MessageReceiver { message, consumer ->
            val base64String = message.data.toStringUtf8()
            logger.debug("[<<] base64String: {}", base64String)
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
            // stop receiving messages
            // subscriber?.stopAsync()
        }
    }
}