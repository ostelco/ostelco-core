package org.ostelco.prime.analytics.publishers

import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.Timestamps
import com.google.pubsub.v1.PubsubMessage
import org.ostelco.analytics.api.ActiveUsersInfo
import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.metrics.api.User
import java.time.Instant

/**
 * This class publishes the active users information events to the Google Cloud Pub/Sub.
 */
object ActiveUsersPublisher :
        PubSubPublisher by DelegatePubSubPublisher(topicId = ConfigRegistry.config.activeUsersTopicId) {

    private val jsonPrinter = JsonFormat.printer().includingDefaultValueFields()

    private fun convertToJson(activeUsersInfo: ActiveUsersInfo): ByteString =
            ByteString.copyFromUtf8(jsonPrinter.print(activeUsersInfo))

    fun publish(userList: List<User>) {
        val timestamp = Instant.now().toEpochMilli()
        val activeUsersInfoBuilder = ActiveUsersInfo.newBuilder().setTimestamp(Timestamps.fromMillis(timestamp))
        for (user in userList) {
            val userBuilder = org.ostelco.analytics.api.User.newBuilder()
            activeUsersInfoBuilder.addUsers(userBuilder.setApn(user.apn).setMccMnc(user.mccMnc).setMsisdn(user.msisdn).build())
        }

        val pubsubMessage = PubsubMessage.newBuilder()
                .setData(convertToJson(activeUsersInfoBuilder.build()))
                .build()

        // schedule a message to be published, messages are automatically batched
        publishPubSubMessage(pubsubMessage)
    }
}
