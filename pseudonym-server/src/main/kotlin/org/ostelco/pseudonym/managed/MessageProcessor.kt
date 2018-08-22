package org.ostelco.pseudonym.managed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.protobuf.util.Timestamps
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import io.dropwizard.lifecycle.Managed
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.ostelco.analytics.grpc.api.DataTrafficInfo
import org.ostelco.prime.model.PseudonymEntity
import org.ostelco.pseudonym.resources.DateBounds
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import javax.ws.rs.client.Client


/**
 * This class converts the Plain DataTrafficInfo message to
 * a pseudonymous version. Pushes the new message
 * to different PubSub topic.
 */

class MessageProcessor(private val subscriptionName: ProjectSubscriptionName,
                       private val publisherTopicName: ProjectTopicName,
                       private val pseudonymEndpoint: String,
                       private val dateBounds: DateBounds,
                       private val client: Client) : Managed {

    private val logger = LoggerFactory.getLogger(MessageProcessor::class.java)
    private val receiver: MessageReceiver
    private var subscriber: Subscriber? = null
    private var publisher: Publisher? = null
    val mapper = jacksonObjectMapper()
    val pseudonymCache: Cache<String, PseudonymEntity>

    // Testing helpers.
    val emulatorHost: String? = System.getenv("PUBSUB_EMULATOR_HOST")
    var channel: ManagedChannel? = null

    init {
        receiver = MessageReceiver { message, consumer ->
            handleMessage(message, consumer)
        }
        pseudonymCache = CacheBuilder.newBuilder()
                .maximumSize(5000)
                .build()
    }

    @Throws(Exception::class)
    override fun start() {
        logger.info("Starting MessageProcessor...")
        if (emulatorHost != null && !emulatorHost.isEmpty()) {
            // Setup for picking up emulator settings
            // https://cloud.google.com/pubsub/docs/emulator#pubsub-emulator-java
            channel = ManagedChannelBuilder.forTarget(emulatorHost).usePlaintext().build()
            val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
            val credentialsProvider = NoCredentialsProvider.create()
            publisher = Publisher.newBuilder(publisherTopicName)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build()
            subscriber = Subscriber.newBuilder(subscriptionName, receiver)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build()

        } else {
            // Production, connect to real pubsub host
            publisher = Publisher.newBuilder(publisherTopicName).build()
            subscriber = Subscriber.newBuilder(subscriptionName, receiver).build()
        }
        subscriber?.startAsync()
    }

    @Throws(Exception::class)
    override fun stop() {
        logger.info("Stopping MessageProcessor...")
        channel?.shutdown()
        subscriber?.stopAsync()
        publisher?.shutdown()
    }

    private fun getPseudonymUrl(msisdn: String, timestamp: Long): String {
        return "$pseudonymEndpoint/pseudonym/get/$msisdn/$timestamp"
    }

    private fun getPseudonymEntity(msisdn: String, timestamp: Long): PseudonymEntity? {
        val (_, keyPrefix) = dateBounds.getBoundsNKeyPrefix(msisdn, timestamp)
        try {
            // Retrieves the element from cache.
            // Incase of cache miss, get the entity via a REST call
            return pseudonymCache.get(keyPrefix, Callable {
                val url = getPseudonymUrl(msisdn, timestamp)
                val target = client.target(url)
                val response = target.request().get()
                if (response.getStatus() != 200) {
                    val unexpectedResponse = response.readEntity(String::class.java)
                    logger.warn("$url returned ${response.getStatus()} Response: $unexpectedResponse")
                    throw javax.ws.rs.ProcessingException(unexpectedResponse)
                }
                logger.warn("$url returned ${response.getStatus()}")
                val json = response.readEntity(String::class.java)
                response.close()
                mapper.readValue<PseudonymEntity>(json)
            })
        } catch (e: ExecutionException) {
            logger.warn("getPseudonymEntity failed, ${e.toString()}")
        }
        return null
    }

    private fun handleMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
        val trafficInfo = DataTrafficInfo.parseFrom(message.data)
        // Retrieve the pseudonym for msisdn
        val pseudonymEntity = getPseudonymEntity(trafficInfo.msisdn, Timestamps.toMillis(trafficInfo.timestamp))
        if (pseudonymEntity == null) {
            logger.error("Error converting DataTrafficInfo message ${message.messageId}")
            consumer.nack()
            return
        }
        // New message with pseudonym msisdn
        val data = DataTrafficInfo.newBuilder()
                .setMsisdn(pseudonymEntity.pseudonym)
                .setBucketBytes(trafficInfo.bucketBytes)
                .setBundleBytes(trafficInfo.bundleBytes)
                .setTimestamp(trafficInfo.timestamp)
                .build()
                .toByteString()
        logger.info("msisdn {}, bucketBytes {}", trafficInfo.msisdn, trafficInfo.bucketBytes)
        val pubsubMessage = PubsubMessage.newBuilder()
                .setData(data)
                .build()

        //schedule a message to be published, messages are automatically batched
        val future = publisher?.publish(pubsubMessage)
        // add an asynchronous callback to handle success / failure
        ApiFutures.addCallback(future, object : ApiFutureCallback<String> {

            override fun onFailure(throwable: Throwable) {
                if (throwable is ApiException) {
                    // details on the API exception
                    logger.warn("Status code: {}", throwable.statusCode.code)
                    logger.warn("Retrying: {}", throwable.isRetryable)
                }
                logger.warn("Error publishing message for msisdn: {}", trafficInfo.msisdn)
                consumer.nack()
            }

            override fun onSuccess(messageId: String) {
                // Once published, returns server-assigned message ids (unique within the topic)
                logger.debug(messageId)
                logger.info("Processed message $messageId")
                consumer.ack()
            }
        })

    }
}

