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
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import io.dropwizard.lifecycle.Managed
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.ostelco.ocs.api.DataTrafficInfo
import org.ostelco.pseudonym.resources.PseudonymEntity
import org.slf4j.LoggerFactory
import javax.ws.rs.client.Client


/**
 * Class representing the Pseudonym entity.
 */

class MessageProcessor(private val subscriptionName: ProjectSubscriptionName,
                       private val publisherTopicName: ProjectTopicName,
                       private val pseudonymEndpoint: String,
                       private val client: Client) : Managed {

    private val LOG = LoggerFactory.getLogger(MessageProcessor::class.java)
    private val receiver: MessageReceiver
    private var subscriber: Subscriber? = null
    private var publisher: Publisher? = null
    val mapper = jacksonObjectMapper()
    // Testing helpers.
    val emulatorHost: String? = System.getenv("PUBSUB_EMULATOR_HOST")
    var channel: ManagedChannel? = null

    init {
        receiver = MessageReceiver { message, consumer ->
            handleMessage(message, consumer)
        }
    }

    @Throws(Exception::class)
    override fun start() {
        LOG.info("Starting MessageProcessor...")
        if (emulatorHost != null && !emulatorHost.isEmpty()) {
            // Setup for picking up emulator settings
            // https://cloud.google.com/pubsub/docs/emulator#pubsub-emulator-java
            channel = ManagedChannelBuilder.forTarget(emulatorHost).usePlaintext(true).build()
            val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
            val credentialsProvider = NoCredentialsProvider.create()
            publisher = Publisher.newBuilder(publisherTopicName)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build();
            subscriber = Subscriber.newBuilder(subscriptionName, receiver)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build();

        } else {
            // Production, connect to real pubsub host
            publisher = Publisher.newBuilder(publisherTopicName).build();
            subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
        }
        subscriber?.startAsync()
    }

    @Throws(Exception::class)
    override fun stop() {
        LOG.info("Stopping MessageProcessor...")
        channel?.shutdown()
        subscriber?.stopAsync()
        publisher?.shutdown()
    }

    private fun getPseudonymUrl(msisdn: String, timestamp: Timestamp): String {
        return "${pseudonymEndpoint}/pseudonym/get/$msisdn/${Timestamps.toMillis(timestamp)}"
    }

    private fun handleMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
        val trafficInfo = DataTrafficInfo.parseFrom(message.data)
        // Retrieve the pseudonym for msisdn
        val url = getPseudonymUrl(trafficInfo.msisdn, trafficInfo.timestamp)
        try {
            val target = client.target(url)
            val response = target.request().get()
            if (response.getStatus() != 200) {
                val unexpectedResponse = response.readEntity(String::class.java)
                LOG.warn("$url returned ${response.getStatus()} Response: ${unexpectedResponse}")
                consumer.nack()
                return
            }
            val json = response.readEntity(String::class.java)
            response.close()
            val pseudonymEntity = mapper.readValue<PseudonymEntity>(json)

            // New message with pseudonym msisdn
            val data = DataTrafficInfo.newBuilder()
                    .setMsisdn(pseudonymEntity.pseudonym)
                    .setBucketBytes(trafficInfo.bucketBytes)
                    .setBundleBytes(trafficInfo.bundleBytes)
                    .setTimestamp(trafficInfo.timestamp)
                    .build()
                    .toByteString()
            LOG.info("msisdn {}, bucketBytes {}", trafficInfo.msisdn, trafficInfo.bucketBytes)
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
                        LOG.warn("Status code: {}", throwable.statusCode.code)
                        LOG.warn("Retrying: {}", throwable.isRetryable)
                    }
                    LOG.warn("Error publishing message for msisdn: {}", trafficInfo.msisdn)
                    consumer.nack()
                }

                override fun onSuccess(messageId: String) {
                    // Once published, returns server-assigned message ids (unique within the topic)
                    LOG.debug(messageId)
                    LOG.info("Processed message $messageId")
                    consumer.ack()
                }
            })
        }
        catch (e: javax.ws.rs.ProcessingException) {
            LOG.error("Error converting DataTrafficInfo message ${message.messageId}, ${e.toString()}")
            consumer.nack()
        }

    }
}

