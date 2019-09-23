package org.ostelco.prime.analytics

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.hibernate.validator.constraints.NotBlank
import org.ostelco.prime.analytics.metrics.CustomMetricsRegistry
import org.ostelco.prime.analytics.publishers.*
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("analytics")
class AnalyticsModule : PrimeModule {

    @JsonProperty("config")
    fun setConfig(config: AnalyticsConfig) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {

        CustomMetricsRegistry.init(env.metrics())

        val server = AnalyticsGrpcServer(8083, AnalyticsGrpcService())

        env.lifecycle().manage(server)

        // dropwizard starts Analytics events publisher
        env.lifecycle().manage(DataConsumptionInfoPublisher)
        env.lifecycle().manage(PurchaseInfoPublisher)
        env.lifecycle().manage(ActiveUsersPublisher)
        env.lifecycle().manage(SimProvisioningPublisher)
        env.lifecycle().manage(SubscriptionStatusUpdatePublisher)
    }
}

data class AnalyticsConfig(
    @NotBlank
    @JsonProperty("projectId")
    val projectId: String,

    @NotBlank
    @JsonProperty("dataTrafficTopicId")
    val dataTrafficTopicId: String,

    @NotBlank
    @JsonProperty("purchaseInfoTopicId")
    val purchaseInfoTopicId: String,

    @NotBlank
    @JsonProperty("activeUsersTopicId")
    val activeUsersTopicId: String,

    @NotBlank
    @JsonProperty("simProvisioningTopicId")
    val simProvisioningTopicId: String,

    @NotBlank
    @JsonProperty("subscriptionStatusUpdateTopicId")
    val subscriptionStatusUpdateTopicId: String
)

object ConfigRegistry {
    lateinit var config: AnalyticsConfig
}