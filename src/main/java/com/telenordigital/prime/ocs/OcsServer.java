package com.telenordigital.prime.ocs;

import io.dropwizard.lifecycle.Managed;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This is OCS Server running on gRPC protocol.
 * Its startup and shutdown are managed by Dropwizard's lifecycle.
 *
 * @author Vihang Patil <vihang.patil@telenordigital.com>
 */
public class OcsServer implements Managed {

    private static final Logger LOG = LoggerFactory.getLogger(OcsServer.class);

    private final int port;
    private final Server server;

    public OcsServer(final int port, final BindableService service) {
        this.port = port;

        // may add Transport Security with Certificates if needed.
        // may add executor for control over number of threads
        server = ServerBuilder
                .forPort(port)
                .addService(service)
                .build();
    }

    /**
     * Startup is managed by Dropwizard's lifecycle.
     *
     * @throws IOException
     */
    @Override
    public void start() throws IOException {
        server.start();
        LOG.info("Server started, listening on {}", port);
    }

    /**
     * Shutdown is managed by Dropwizard's lifecycle.
     *
     * @throws InterruptedException
     */
    @Override
    public void stop() throws InterruptedException {
        if (server != null) {
            LOG.info("Stopping Server running on {}", port);
            server.shutdown();
            blockUntilShutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
