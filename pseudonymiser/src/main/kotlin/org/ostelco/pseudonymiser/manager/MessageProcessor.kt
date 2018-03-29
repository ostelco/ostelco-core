package org.ostelco.pseudonymiser.manager

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.rpc.ApiException
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
import javax.ws.rs.client.Client
import org.ostelco.ocs.api.DataTrafficInfo
import org.slf4j.LoggerFactory
import java.time.Instant
import javax.ws.rs.core.Response

/**
 * Class representing the Pseudonym entity.
 */
data class PseudonymEntity(val msisdn: String, val pseudonym: String, val start: Long, val end: Long)


class MessageProcessor(private val subscriptionName: ProjectSubscriptionName,
                       private val publisherTopicName: ProjectTopicName,
                       private val pseudonymEndpoint: String,
                       private val client: Client) : Managed {

    private val LOG = LoggerFactory.getLogger(MessageProcessor::class.java)
    private val receiver: MessageReceiver
    private var subscriber: Subscriber? = null
    private var publisher: Publisher? = null
    val mapper = jacksonObjectMapper()

    init {
        receiver = MessageReceiver { message, consumer ->
            handleMessage(message, consumer)
        }
    }

    @Throws(Exception::class)
    override fun start() {
        LOG.info("Starting MessageProcessor...")
        publisher = Publisher.newBuilder(publisherTopicName).build();
        subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
        subscriber!!.startAsync()
    }

    @Throws(Exception::class)
    override fun stop() {
        LOG.info("Stopping MessageProcessor...")
        if (subscriber != null) {
            subscriber!!.stopAsync()
        }
        if (publisher != null) {
            publisher!!.shutdown()
        }
    }

    private fun getPseudonymUrl(msisdn: String, timestamp: Timestamp): String {
        return "${pseudonymEndpoint}/pseudonym/get/$msisdn/${Timestamps.toMillis(timestamp)}"
    }

    private fun handleMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
        val trafficInfo = DataTrafficInfo.parseFrom(message.data)

        // Retrieve the pseudonym for msisdn
        val url = getPseudonymUrl(trafficInfo.msisdn, trafficInfo.timestamp)
        val target = client.target(url)
        val response = target.request().get()
        if (response.getStatus() !== 200) {
            val unexpectedResponse = response.readEntity(String::class.java)
            LOG.warn("$url returned ${response.getStatus()} Response: ${unexpectedResponse}")
            consumer.nack()
            return
        }
        var json = response.readEntity(String::class.java)
        var pseudonymEntity = mapper.readValue<PseudonymEntity>(json)

        // New message with pseudonym msisdn
        val data = DataTrafficInfo.newBuilder()
                .setMsisdn(pseudonymEntity.pseudonym)
                .setBucketBytes(trafficInfo.bucketBytes)
                .setBundleBytes(trafficInfo.bundleBytes)
                .setTimestamp(trafficInfo.timestamp)
                .build()
                .toByteString()
        LOG.info("Received messages ")
        LOG.info("msisdn {}, bucketBytes {}", trafficInfo.msisdn, trafficInfo.bucketBytes)
        val pubsubMessage = PubsubMessage.newBuilder()
                .setData(data)
                .build()

        //schedule a message to be published, messages are automatically batched
        val future = publisher!!.publish(pubsubMessage)

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
                consumer.ack()
            }
        })

    }
}

