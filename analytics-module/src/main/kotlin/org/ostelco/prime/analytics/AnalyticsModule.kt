package org.ostelco.prime.analytics

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import com.codahale.metrics.SharedMetricRegistries
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.analytics.metrics.OcsgwMetrics

@JsonTypeName("analytics")
class AnalyticsModule : PrimeModule {

    @JsonProperty("config")
    lateinit var config: AnalyticsConfig

    override fun init(env: Environment) {

        val ocsgwMetrics = OcsgwMetrics(SharedMetricRegistries.getOrCreate("ocsgw-metrics"))

        val server = AnalyticsGrpcServer(8082, AnalyticsGrpcService(ocsgwMetrics))

        env.lifecycle().manage(server)
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