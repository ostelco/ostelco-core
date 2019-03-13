package org.ostelco.ocsgw.datasource.protobuf

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.ApiException
import com.google.api.gax.rpc.FixedTransportChannelProvider
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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService


class PubSubDataSource(
        private val asyncDataSource: ProtobufDataSource,
        projectId: String,
        topicId: String,
        ccrSubscriptionId: String,
        activateSubscriptionId: String) : DataSource {

    private val logger by getLogger()

    private var singleThreadScheduledExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var publisher: Publisher

    init {

        // init publisher
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

        // Instantiate an asynchronous message receiver
        setupPubSubSubscriber(projectId, ccrSubscriptionId) { message, consumer ->
            // handle incoming message, then ack/nack the received message
            asyncDataSource.handleProtobufCcrAnswer(
                    CreditControlAnswerInfo.parseFrom(message))
            consumer.ack()
        }

        setupPubSubSubscriber(projectId, activateSubscriptionId) { message, consumer ->
            // handle incoming message, then ack/nack the received message
            asyncDataSource.handleProtobufActivateResponse(
                    ActivateResponse.parseFrom(message))
            consumer.ack()
        }
    }

    override fun init() {

    }

    override fun handleRequest(context: CreditControlContext) {

        val creditControlRequestInfo = asyncDataSource.handleRequest(context)

        if (creditControlRequestInfo != null) {
            val pubsubMessage = PubsubMessage.newBuilder()
                    .setData(creditControlRequestInfo.toByteString())
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
                    logger.warn("Error publishing active users list")
                }

                override fun onSuccess(messageId: String) {
                    // Once published, returns server-assigned message ids (unique within the topic)
                    logger.debug(messageId)
                }
            }, singleThreadScheduledExecutor)
        }
    }

    override fun isBlocked(msisdn: String): Boolean = asyncDataSource.isBlocked(msisdn)

    private fun setupPubSubSubscriber(projectId: String, subscriptionId: String, handler: (ByteString, AckReplyConsumer) -> Unit) {
        // init subscriber
        val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)

        val receiver = MessageReceiver { message, consumer ->
            handler(message.data, consumer)
        }

        var subscriber: Subscriber? = null
        try {
            // Create a subscriber for "my-subscription-id" bound to the message receiver
            subscriber = Subscriber.newBuilder(subscriptionName, receiver)
                    .build()
                    .also { it.startAsync() }
        } finally {
            // stop receiving messages
            subscriber?.stopAsync()
        }
    }
}