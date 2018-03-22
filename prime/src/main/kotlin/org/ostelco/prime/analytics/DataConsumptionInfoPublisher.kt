package org.ostelco.prime.analytics

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.rpc.ApiException
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.util.Timestamps
import com.google.pubsub.v1.PubsubMessage
import com.google.pubsub.v1.TopicName
import com.lmax.disruptor.EventHandler
import io.dropwizard.lifecycle.Managed
import org.ostelco.ocs.api.DataTrafficInfo
import org.ostelco.prime.disruptor.PrimeEvent
import org.ostelco.prime.disruptor.PrimeEventMessageType.CREDIT_CONTROL_REQUEST
import org.ostelco.prime.logger
import java.io.IOException
import java.time.Instant

/**
 * This class publishes the data consumption information events to the Google Cloud Pub/Sub.
 */
class DataConsumptionInfoPublisher(private val projectId: String, private val topicId: String) : EventHandler<PrimeEvent>, Managed {

    private val LOG by logger()

    private var publisher: Publisher? = null

    @Throws(IOException::class)
    override fun start() {

        val topicName = TopicName.of(projectId, topicId)

        // Create a publisher instance with default settings bound to the topic
        publisher = Publisher.newBuilder(topicName).build()
    }

    @Throws(Exception::class)
    override fun stop() {
        if (publisher != null) {
            // When finished with the publisher, shutdown to free up resources.
            publisher!!.shutdown()
        }
    }

    override fun onEvent(
            event: PrimeEvent,
            sequence: Long,
            endOfBatch: Boolean) {

        if (event.messageType != CREDIT_CONTROL_REQUEST) {
            return
        }

        // FiXMe : We only report the requested bucket. Should probably report the Used-Units instead
        val data = DataTrafficInfo.newBuilder()
                .setMsisdn(event.msisdn)
                .setBucketBytes(event.requestedBucketBytes)
                .setBundleBytes(event.bundleBytes)
                .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                .build()
                .toByteString()

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
                LOG.warn("Error publishing message for msisdn: {}", event.msisdn)
            }

            override fun onSuccess(messageId: String) {
                // Once published, returns server-assigned message ids (unique within the topic)
                LOG.debug(messageId)
            }
        })
    }
}
