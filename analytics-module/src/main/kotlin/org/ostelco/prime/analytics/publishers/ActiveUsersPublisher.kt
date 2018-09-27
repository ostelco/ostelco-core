package org.ostelco.prime.analytics.publishers

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.rpc.ApiException
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.protobuf.ByteString
import com.google.protobuf.util.Timestamps
import com.google.pubsub.v1.PubsubMessage
import org.ostelco.analytics.api.ActiveUsersInfo
import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.getLogger
import org.ostelco.prime.metrics.api.User
import org.ostelco.prime.module.getResource
import org.ostelco.prime.pseudonymizer.PseudonymizerService
import java.time.Instant

/**
 * This class publishes the active users information events to the Google Cloud Pub/Sub.
 */
object ActiveUsersPublisher :
        PubSubPublisher by DelegatePubSubPublisher(topicId = ConfigRegistry.config.activeUsersTopicId) {

    private val logger by getLogger()

    private val pseudonymizerService by lazy { getResource<PseudonymizerService>() }

    private var gson: Gson = GsonBuilder().create()

    private fun convertToJson(activeUsersInfo: ActiveUsersInfo): ByteString =
            ByteString.copyFromUtf8(gson.toJson(activeUsersInfo))


    fun publish(userList: List<User>) {
        val timestamp = Instant.now().toEpochMilli()
        val activeUsersInfoBuilder = ActiveUsersInfo.newBuilder().setTimestamp(Timestamps.fromMillis(timestamp))
        for (user in userList) {
            val userBuilder = org.ostelco.analytics.api.User.newBuilder()
            val pseudonym = pseudonymizerService.getSubscriberIdPseudonym(user.msisdn, timestamp).pseudonym
            activeUsersInfoBuilder.addUsers(userBuilder.setApn(user.apn).setMncMcc(user.mncMcc).setMsisdn(pseudonym).build())
        }

        val pubsubMessage = PubsubMessage.newBuilder()
                .setData(convertToJson(activeUsersInfoBuilder.build()))
                .build()

        //schedule a message to be published, messages are automatically batched
        val future = publishPubSubMessage(pubsubMessage)

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
