package org.ostelco.prime.analytics.publishers

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.rpc.ApiException
import com.google.cloud.pubsub.v1.Publisher
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import io.dropwizard.lifecycle.Managed
import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.logger
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.PurchaseRecordInfo
import org.ostelco.prime.module.getResource
import org.ostelco.prime.pseudonymizer.PseudonymizerService
import java.io.IOException
import java.net.URLEncoder


/**
 * This class publishes the purchase information events to the Google Cloud Pub/Sub.
 */
object PurchaseInfoPublisher : Managed {

    private val logger by logger()

    private val pseudonymizerService by lazy { getResource<PseudonymizerService>() }

    private var gson: Gson = createGson()

    private lateinit var publisher: Publisher

    @Throws(IOException::class)
    override fun start() {

        val topicName = ProjectTopicName.of(ConfigRegistry.config.projectId, ConfigRegistry.config.purchaseInfoTopicId)

        // Create a publisher instance with default settings bound to the topic
        publisher = Publisher.newBuilder(topicName).build()
    }

    @Throws(Exception::class)
    override fun stop() {
        // When finished with the publisher, shutdown to free up resources.
        publisher.shutdown()
    }

    private fun createGson(): Gson {
        val builder = GsonBuilder()
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        val serializer = JsonSerializer<Map<String, String>> { src, _, _ ->
            val array = JsonArray()
            src.forEach { k, v ->
                val property = JsonObject()
                property.addProperty("key", k)
                property.addProperty("value", v)
                array.add(property)
            }
            array
        }
        builder.registerTypeAdapter(mapType, serializer)
        return builder.create()
    }

    private fun convertToJson(purchaseRecordInfo: PurchaseRecordInfo): ByteString =
            ByteString.copyFromUtf8(gson.toJson(purchaseRecordInfo))


    fun publish(purchaseRecord: PurchaseRecord, subscriberId: String, status: String) {

        val encodedSubscriberId = URLEncoder.encode(subscriberId,"UTF-8")
        val pseudonym = pseudonymizerService.getMsisdnPseudonymEntityFor(encodedSubscriberId, purchaseRecord.timestamp).pseudonym

        val pubsubMessage = PubsubMessage.newBuilder()
                .setData(convertToJson(PurchaseRecordInfo(purchaseRecord, pseudonym, status)))
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
                logger.warn("Error publishing purchase record for msisdn: {}", purchaseRecord.msisdn)
            }

            override fun onSuccess(messageId: String) {
                // Once published, returns server-assigned message ids (unique within the topic)
                logger.debug(messageId)
            }
        })
    }
}
