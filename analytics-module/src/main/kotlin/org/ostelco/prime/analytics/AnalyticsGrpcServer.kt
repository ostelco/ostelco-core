package org.ostelco.prime.analytics

import io.dropwizard.lifecycle.Managed
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import org.ostelco.prime.logger
import java.io.IOException

/**
 * This is Analytics Server running on gRPC protocol.
 * Its startup and shutdown are managed by Dropwizard's lifecycle
 * through the Managed interface.
 *
 */
class AnalyticsGrpcServer(private val port: Int, service: BindableService) : Managed {

    private val logger by logger()

    // may add Transport Security with Certificates if needed.
    // may add executor for control over number of threads
    private val server: Server = ServerBuilder.forPort(port).addService(service).build()

    /**
     * Startup is managed by Dropwizard's lifecycle.
     *
     * @throws IOException ... sometimes, perhaps.
     */
    override fun start() {
        server.start()
        logger.info("Analytics Server started, listening for incoming gRPC traffic on {}", port)
    }

    /**
     * Shutdown is managed by Dropwizard's lifecycle.
     *
     * @throws InterruptedException When something goes wrong.
     */
    override fun stop() {
        logger.info("Stopping Analytics Server listening for gRPC traffic on  {}", port)
        server.shutdown()
        blockUntilShutdown()
    }

    /**
     * Used for unit testing
     */
    fun forceStop() {
        logger.info("Stopping forcefully Analytics Server listening for gRPC traffic on  {}", port)
        server.shutdownNow()
    }

    private fun blockUntilShutdown() {
        server.awaitTermination()
    }
}
