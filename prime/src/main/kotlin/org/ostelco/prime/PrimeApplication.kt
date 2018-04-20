package org.ostelco.prime

import io.dropwizard.Application
import io.dropwizard.setup.Environment
import org.ostelco.importer.ImportDeclaration
import org.ostelco.importer.ImportProcessor
import org.ostelco.importer.ImporterResource
import org.ostelco.prime.analytics.DataConsumptionInfoPublisher
import org.ostelco.prime.config.PrimeConfiguration
import org.ostelco.prime.disruptor.ClearingEventHandler
import org.ostelco.prime.disruptor.PrimeDisruptor
import org.ostelco.prime.disruptor.PrimeEventProducer
import org.ostelco.prime.events.EventListeners
import org.ostelco.prime.events.EventProcessor
import org.ostelco.prime.events.OcsBalanceUpdaterImpl
import org.ostelco.prime.firebase.FbStorage
import org.ostelco.prime.ocs.OcsServer
import org.ostelco.prime.ocs.OcsService
import org.ostelco.prime.ocs.OcsState

class PrimeApplication : Application<PrimeConfiguration>() {

    private val LOG by logger()

    @Throws(Exception::class)
    override fun run(
            primeConfiguration: PrimeConfiguration,
            environment: Environment) {

        val disruptor = PrimeDisruptor()

        // Disruptor provides RingBuffer, which is used by Producer
        val producer = PrimeEventProducer(disruptor.disruptor.ringBuffer)

        // OcsService uses Producer to produce events for incoming requests from P-GW
        val ocsService = OcsService(producer)

        // OcsServer assigns OcsService as handler for gRPC requests
        val server = OcsServer(8082, ocsService.asOcsServiceImplBase())

        val ocsState = OcsState()

        val eventProcessorConfig = primeConfiguration.eventProcessorConfig

        // XXX Badly named class with less than clearly specified intent.
        //     What it's doing is to glue things together and thus
        //     concentrate coupling between other classes into this
        //     single class, but that isn't well documented yet.
        val eventListeners = EventListeners(ocsState)

        val storage = FbStorage(
                eventProcessorConfig.projectId,
                eventProcessorConfig.configFile,
                eventListeners)

        val ocsBalanceUpdater = OcsBalanceUpdaterImpl(producer)
        val eventProcessor = EventProcessor(storage, ocsBalanceUpdater)

        val dataConsumptionInfoPublisher = DataConsumptionInfoPublisher(
                eventProcessorConfig.projectId,
                eventProcessorConfig.topicId)

        // Events flow:
        //      Producer:(OcsService, Subscriber)
        //          -> Handler:(OcsState)
        //              -> Handler:(OcsService, Subscriber, AnalyticsPublisher)
        //                  -> Clear

        disruptor.disruptor.handleEventsWith(ocsState).then(ocsService.asEventHandler(), eventProcessor, dataConsumptionInfoPublisher).then(ClearingEventHandler())


        // Add the importer and an import processor
        val importProcessor = object: ImportProcessor {
            override fun import(decl: ImportDeclaration) : Boolean {
                LOG.info("Imported declaration..")
                return true
            }
        }

        val importer  = ImporterResource(importProcessor)

        // Wire up the importer web resource
        environment.jersey().register(importer)

        val lifecycle = environment.lifecycle()
        // dropwizard starts Analytics events publisher
        lifecycle.manage(dataConsumptionInfoPublisher)
        // dropwizard starts event processor
        lifecycle.manage(eventProcessor)
        // dropwizard starts disruptor
        lifecycle.manage(disruptor)
        // dropwizard starts server
        lifecycle.manage(server)
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            PrimeApplication().run(*args)
        }
    }
}
