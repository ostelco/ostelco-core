package org.ostelco.prime.analytics

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.analytics.metrics.CustomMetricsRegistry
import org.ostelco.prime.analytics.publishers.DataConsumptionInfoPublisher
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
    }
}

class AnalyticsConfig {
    @NotEmpty
    @JsonProperty("projectId")
    lateinit var projectId: String

    @NotEmpty
    @JsonProperty("topicId")
    lateinit var topicId: String
}

object ConfigRegistry {
    lateinit var config: AnalyticsConfig
}