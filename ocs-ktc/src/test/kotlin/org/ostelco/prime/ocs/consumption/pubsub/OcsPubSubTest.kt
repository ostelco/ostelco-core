package org.ostelco.prime.ocs.consumption.pubsub

import arrow.core.getOrElse
import com.google.cloud.pubsub.v1.Publisher
import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks
import org.joda.time.Duration
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.prime.getLogger
import org.ostelco.prime.jersey.client.put
import org.ostelco.prime.ocs.core.OnlineCharging
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

private const val PROJECT_ID = "dummyGcpProjectId"
private const val TARGET = "http://0.0.0.0:8085"
private const val CCR_TOPIC = "ocs-ccr"
private const val CCA_TOPIC = "ocs-cca"
private const val ACTIVATE_TOPIC = "ocs-activate"
private const val CCR_SUBSCRIPTION = "ocs-ccr-sub"
private const val CCA_SUBSCRIPTION = "ocsgw-cca-sub"
private const val ACTIVATE_SUBSCRIPTION = "ocsgw-activate-sub"

@ExperimentalUnsignedTypes
class OcsPubSubTest {

    private val logger by getLogger()

    private lateinit var pubSubClient: PubSubClient
    private lateinit var publisher: Publisher

    @Before
    fun `setup Topics and Subscriptions`() {

        createTopicWithSubscription(CCR_TOPIC, CCR_SUBSCRIPTION)
        createTopicWithSubscription(CCA_TOPIC, CCA_SUBSCRIPTION)
        createTopicWithSubscription(ACTIVATE_TOPIC, ACTIVATE_SUBSCRIPTION)

        System.setProperty("PUBSUB_EMULATOR_HOST", "0.0.0.0:8085")

        pubSubClient = PubSubClient(
                ocsAsyncRequestConsumer = OnlineCharging,
                projectId = PROJECT_ID,
                activateTopicId = ACTIVATE_TOPIC,
                ccrSubscriptionId = CCR_SUBSCRIPTION)

        pubSubClient.start()
    }

    @After
    fun stop() {
        if (::pubSubClient.isInitialized) {
            pubSubClient.stop()
        }
        if (::publisher.isInitialized) {
            publisher.shutdown()
        }
    }

    @Test
    fun `test OCS over PubSub`() {

        val cdl = CountDownLatch(1)
        pubSubClient.setupPubSubSubscriber(CCA_SUBSCRIPTION) { message, consumer ->
            val ccaInfo = CreditControlAnswerInfo.parseFrom(message)
            logger.info("Received CCA - {}", ccaInfo)
            cdl.countDown()
            consumer.ack()
        }

        publisher = pubSubClient.setupPublisherToTopic(CCR_TOPIC)

        val requestId = UUID.randomUUID().toString()
        val ccrInfo = CreditControlRequestInfo.newBuilder()
                .setRequestId(requestId)
                .setMsisdn("471234568")
                .setTopicId(CCA_TOPIC)
                .build()

        logger.info("Sending CCR - {}", ccrInfo)
        pubSubClient.publish(
                messageId = requestId,
                byteString = ccrInfo.toByteString(),
                publisher = publisher)

        logger.info("Waiting for CCA")
        assert(cdl.await(15, SECONDS)) { "Failed to received CCA back on PubSub" }
    }

    private fun createTopicWithSubscription(topicId: String, vararg subscriptionIds: String) {
        logger.info("Created topic: {}", createTopic(topicId)?.name)
        subscriptionIds.forEach { subscriptionId ->
            createSubscription(topicId, subscriptionId)?.apply {
                logger.info("Created subscription: {} for topic: {}", this.name, this.topic)
            }
        }
    }

    private fun createTopic(topicId: String) = put<String, CreateTopicResponse> {
        target = TARGET
        path = "v1/projects/$PROJECT_ID/topics/$topicId"
    }.getOrElse { null }

    private fun createSubscription(topicId: String, subscriptionId: String) = put<String, CreateSubscriptionResponse> {
        target = TARGET
        path = "v1/projects/$PROJECT_ID/subscriptions/$subscriptionId"
        body = """{"topic":"projects/$PROJECT_ID/topics/$topicId"}"""
    }.getOrElse { null }

    companion object {

        @ClassRule
        @JvmField
        var docker: DockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/docker-compose.yaml")
                .waitingForService("pubsub-emulator", HealthChecks.toHaveAllPortsOpen())
                .waitingForService("pubsub-emulator",
                        HealthChecks.toRespond2xxOverHttp(8085) { port ->
                            port.inFormat("http://\$HOST:\$EXTERNAL_PORT/")
                        },
                        Duration.standardSeconds(40L))
                .build()
    }
}

data class CreateTopicResponse(var name: String)

data class CreateSubscriptionResponse(
        val name: String,
        val topic: String,
        val pushConfig: Any,
        val ackDeadlineSeconds: Long,
        val messageRetentionDuration: String)
