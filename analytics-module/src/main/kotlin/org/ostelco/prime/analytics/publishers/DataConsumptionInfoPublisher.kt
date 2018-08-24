package org.ostelco.prime.analytics.publishers

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.rpc.ApiException
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.util.Timestamps
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import io.dropwizard.lifecycle.Managed
import org.ostelco.analytics.api.DataTrafficInfo
import org.ostelco.prime.analytics.ConfigRegistry.config
import org.ostelco.prime.logger
import java.io.IOException
import java.time.Instant

/**
 * This class publishes the data consumption information events to the Google Cloud Pub/Sub.
 */
object DataConsumptionInfoPublisher : Managed {

    private val logger by logger()

    private lateinit var publisher: Publisher

    @Throws(IOException::class)
    override fun start() {

        val topicName = ProjectTopicName.of(config.projectId, config.topicId)

        // Create a publisher instance with default settings bound to the topic
        publisher = Publisher.newBuilder(topicName).build()
    }

    @Throws(Exception::class)
    override fun stop() {
        // When finished with the publisher, shutdown to free up resources.
        publisher.shutdown()
    }

    fun publish(msisdn: String, usedBucketBytes: Long, bundleBytes: Long) {

        val data = DataTrafficInfo.newBuilder()
                .setMsisdn(msisdn)
                .setBucketBytes(usedBucketBytes)
                .setBundleBytes(bundleBytes)
                .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                .build()
                .toByteString()

        val pubsubMessage = PubsubMessage.newBuilder()
                .setData(data)
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
                logger.warn("Error publishing message for msisdn: {}", msisdn)
            }

            override fun onSuccess(messageId: String) {
                // Once published, returns server-assigned message ids (unique within the topic)
                logger.debug(messageId)
            }
        })
    }
}
