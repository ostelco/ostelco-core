package org.ostelco.prime.ocs.consumption.grpc

import io.dropwizard.lifecycle.Managed
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import org.ostelco.prime.getLogger

/**
 * This is OCS Server running on gRPC protocol.
 * Its startup and shutdown are managed by Dropwizard's lifecycle
 * through the Managed interface.
 *
 */
class OcsGrpcServer(private val port: Int, service: BindableService) : Managed {

    private val logger by getLogger()

    // may add Transport Security with Certificates if needed.
    // may add executor for control over number of threads
    private val server: Server = ServerBuilder.forPort(port).addService(service).build()

    override fun start() {
        server.start()
        logger.info("OcsServer Server started, listening for incoming gRPC traffic on {}", port)
    }

    override fun stop() {
        logger.info("Stopping OcsServer Server listening for gRPC traffic on  {}", port)
        server.shutdown()
        blockUntilShutdown()
    }

    // Used for unit testing
    fun forceStop() {
        logger.info("Stopping forcefully OcsServer Server listening for gRPC traffic on  {}", port)
        server.shutdownNow()
    }

    private fun blockUntilShutdown() {
        server.awaitTermination()
    }
}
