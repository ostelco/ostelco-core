package org.ostelco.prime.ocs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.ocs.ConfigRegistry.config
import org.ostelco.prime.ocs.consumption.grpc.OcsGrpcServer
import org.ostelco.prime.ocs.consumption.grpc.OcsGrpcService
import org.ostelco.prime.ocs.consumption.pubsub.PubSubClient
import org.ostelco.prime.ocs.core.OnlineCharging
import org.ostelco.prime.ocs.core.Rating

@JsonTypeName("ocs")
class OcsModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: Config) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        env.lifecycle().manage(
                OcsGrpcServer(
                        port = 8082,
                        service = OcsGrpcService(OnlineCharging)))

        config.pubSubChannel?.let { config ->
            env.lifecycle().manage(
                    PubSubClient(
                            ocsAsyncRequestConsumer = OnlineCharging,
                            projectId = config.projectId,
                            activateTopicId = config.activateTopicId,
                            ccrSubscriptionId = config.ccrSubscriptionId))
        }

        config.rating?.forEach { rate ->
            Rating.addRate(rate.serviceId, rate.ratingGroup, rate.rate)
        }
    }
}

data class Rate(
        val serviceId: Long,
        val ratingGroup: Long,
        val rate: String)

data class PubSubChannel(
        val projectId: String,
        val activateTopicId: String,
        val ccrSubscriptionId: String)

data class Config(
        val lowBalanceThreshold: Long = 0,
        val pubSubChannel: PubSubChannel? = null,
        val rating: ArrayList<Rate>? = null)

object ConfigRegistry {
    lateinit var config: Config
}