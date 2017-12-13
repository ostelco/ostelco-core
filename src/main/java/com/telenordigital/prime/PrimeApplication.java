package com.telenordigital.prime;

import com.telenordigital.prime.config.EventProcessorConfiguration;
import com.telenordigital.prime.config.PrimeConfiguration;
import com.telenordigital.prime.disruptor.ClearingEventHandler;
import com.telenordigital.prime.disruptor.PrimeDisruptor;
import com.telenordigital.prime.disruptor.PrimeEventProducer;
import com.telenordigital.prime.events.EventProcessor;
import com.telenordigital.prime.events.OcsBalanceUpdater;
import com.telenordigital.prime.events.OcsBalanceUpdaterImpl;
import com.telenordigital.prime.storage.Storage;
import com.telenordigital.prime.events.EventListeners;
import com.telenordigital.prime.firebase.FbStorage;
import com.telenordigital.prime.ocs.OcsServer;
import com.telenordigital.prime.ocs.OcsService;
import com.telenordigital.prime.ocs.OcsState;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

/**
 * @author Vihang Patil <vihang.patil@telenordigital.com>
 */
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

        // OcsService uses Producer to produce events for incoming requests from PGw
        final OcsService ocsService = new OcsService(producer);

        // OcsServer assigns OcsService as handler for gRPC requests
        final OcsServer server = new OcsServer(8082, ocsService.asOcsServiceImplBase());

        final OcsState ocsState = new OcsState();

        final EventProcessorConfiguration eventProcessorConfig =
                primeConfiguration.getEventProcessorConfig();

        // XXX Badly named class with less than clarly specified intent.
        //     What it it's doing is to glue things together and thus
        //     concentrate coupling between other classes into this
        //     single class, but that isn't well documented yet.
        final EventListeners eventListeners = new EventListeners(ocsState);

        final Storage storage = new FbStorage(
                eventProcessorConfig.getDatabaseName(),
                eventProcessorConfig.getConfigFile(),
                eventListeners);

        final OcsBalanceUpdater ocsBalanceUpdater = new OcsBalanceUpdaterImpl(producer);
        final EventProcessor eventProcessor = new EventProcessor(storage, ocsBalanceUpdater);


        // Events flow:
        //      Producer:(OcsService, Subscriber)
        //          -> Handler:(OcsState)
        //              -> Handler:(OcsService, Subscriber)
        //                  -> Clear
        disruptor.getDisruptor().
                handleEventsWith(ocsState).
                then(ocsService.asEventHandler(), eventProcessor).
                then(new ClearingEventHandler());

        // dropwizard starts event processor
        environment.lifecycle().manage(eventProcessor);

        // dropwizard starts disruptor
        environment.lifecycle().manage(disruptor);
        // dropwizard starts server
        environment.lifecycle().manage(server);
    }
}
