package org.ostelco.prime;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import org.ostelco.prime.analytics.DataConsumptionInfoPublisher;
import org.ostelco.prime.config.EventProcessorConfiguration;
import org.ostelco.prime.config.PrimeConfiguration;
import org.ostelco.prime.disruptor.ClearingEventHandler;
import org.ostelco.prime.disruptor.PrimeDisruptor;
import org.ostelco.prime.disruptor.PrimeEventProducer;
import org.ostelco.prime.events.EventListeners;
import org.ostelco.prime.events.EventProcessor;
import org.ostelco.prime.events.OcsBalanceUpdater;
import org.ostelco.prime.events.OcsBalanceUpdaterImpl;
import org.ostelco.prime.firebase.FbStorage;
import org.ostelco.prime.ocs.OcsServer;
import org.ostelco.prime.ocs.OcsService;
import org.ostelco.prime.ocs.OcsState;
import org.ostelco.prime.storage.Storage;

public final class PrimeApplication extends Application<PrimeConfiguration> {

    public static void main(final String[] args) throws Exception {
        new PrimeApplication().run(args);
    }

    @Override
    public void run(
            final PrimeConfiguration primeConfiguration,
            final Environment environment) throws Exception {

        final PrimeDisruptor disruptor = new PrimeDisruptor();

        // Disruptor provides RingBuffer, which is used by Producer
        final PrimeEventProducer producer =
                new PrimeEventProducer(disruptor.getDisruptor().getRingBuffer());

        // OcsService uses Producer to produce events for incoming requests from P-GW
        final OcsService ocsService = new OcsService(producer);

        // OcsServer assigns OcsService as handler for gRPC requests
        final OcsServer server = new OcsServer(8082, ocsService.asOcsServiceImplBase());

        final OcsState ocsState = new OcsState();

        final EventProcessorConfiguration eventProcessorConfig =
                primeConfiguration.getEventProcessorConfig();

        // XXX Badly named class with less than clearly specified intent.
        //     What it's doing is to glue things together and thus
        //     concentrate coupling between other classes into this
        //     single class, but that isn't well documented yet.
        final EventListeners eventListeners = new EventListeners(ocsState);

        final Storage storage = new FbStorage(
                eventProcessorConfig.getProjectId(),
                eventProcessorConfig.getConfigFile(),
                eventListeners);

        final OcsBalanceUpdater ocsBalanceUpdater = new OcsBalanceUpdaterImpl(producer);
        final EventProcessor eventProcessor = new EventProcessor(storage, ocsBalanceUpdater);

        final DataConsumptionInfoPublisher dataConsumptionInfoPublisher = new DataConsumptionInfoPublisher(
                eventProcessorConfig.getProjectId(),
                eventProcessorConfig.getTopicId());

        // Events flow:
        //      Producer:(OcsService, Subscriber)
        //          -> Handler:(OcsState)
        //              -> Handler:(OcsService, Subscriber, AnalyticsPublisher)
        //                  -> Clear
        //noinspection unchecked
        disruptor.getDisruptor().
                handleEventsWith(ocsState).
                then(ocsService.asEventHandler(), eventProcessor, dataConsumptionInfoPublisher).
                then(new ClearingEventHandler());

        // dropwizard starts Analytics events publisher
        environment.lifecycle().manage(dataConsumptionInfoPublisher);
        // dropwizard starts event processor
        environment.lifecycle().manage(eventProcessor);
        // dropwizard starts disruptor
        environment.lifecycle().manage(disruptor);
        // dropwizard starts server
        environment.lifecycle().manage(server);
    }
}
