package org.ostelco.prime.ocs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.analytics.AnalyticsReporter
import org.ostelco.prime.consumption.OcsGrpcServer
import org.ostelco.prime.consumption.OcsService
import org.ostelco.prime.disruptor.BundleBalanceStore
import org.ostelco.prime.disruptor.ClearingEvent
import org.ostelco.prime.disruptor.EventProducerImpl
import org.ostelco.prime.disruptor.OcsDisruptor
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.thresholds.ThresholdChecker

@JsonTypeName("ocs")
class OcsModule : PrimeModule {

    @JsonProperty("config")
    lateinit var config: OcsConfig

    override fun init(env: Environment) {

        val disruptor = OcsDisruptor()

        // Disruptor provides RingBuffer, which is used by Producer
        val producer = EventProducerImpl(disruptor.disruptor.ringBuffer)

        // OcsSubscriberServiceSingleton uses Producer to produce events for incoming requests from Client App
        OcsPrimeServiceSingleton.init(producer)

        // OcsService uses Producer to produce events for incoming requests from P-GW
        val ocsService = OcsService(producer)

        // OcsServer assigns OcsService as handler for gRPC requests
        val server = OcsGrpcServer(8082, ocsService.ocsGrpcService)

        // Events flow:
        //      Producer:(OcsService, Customer)
        //          -> Handler:(OcsState)
        //              -> Handler:(OcsService, Customer, AnalyticsPublisher)
        //                  -> Clear

        disruptor.disruptor
                .handleEventsWith(OcsState())
                .then(ocsService.eventHandler,
                        BundleBalanceStore(),
                        OcsPrimeServiceSingleton.purchaseRequestHandler,
                        ThresholdChecker(config.lowBalanceThreshold),
                        AnalyticsReporter)
                .then(ClearingEvent)

        // dropwizard starts disruptor
        env.lifecycle().manage(disruptor)
        // dropwizard starts server
        env.lifecycle().manage(server)
    }
}

data class OcsConfig(
    @NotEmpty
    @JsonProperty("lowBalanceThreshold")
    var lowBalanceThreshold: Long = 0)
