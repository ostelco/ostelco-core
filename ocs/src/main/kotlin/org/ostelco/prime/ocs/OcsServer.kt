package org.ostelco.prime.ocs

import io.dropwizard.lifecycle.Managed
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import org.ostelco.prime.logger
import java.io.IOException

/**
 * This is OCS Server running on gRPC protocol.
 * Its startup and shutdown are managed by Dropwizard's lifecycle
 * through the Managed interface.
 *
 */
class OcsServer(private val port: Int, service: BindableService) : Managed {

    private val LOG by logger()

    // may add Transport Security with Certificates if needed.
    // may add executor for control over number of threads
    private val server: Server? = ServerBuilder.forPort(port).addService(service).build()

    /**
     * Startup is managed by Dropwizard's lifecycle.
     *
     * @throws IOException ... sometimes, perhaps.
     */
    @Throws(IOException::class)
    override fun start() {
        server!!.start()
        LOG.info("OcsServer Server started, listening for incoming gRPC traffic on {}", port)
    }

    /**
     * Shutdown is managed by Dropwizard's lifecycle.
     *
     * @throws InterruptedException When something goes wrong.
     */
    @Throws(InterruptedException::class)
    override fun stop() {
        if (server != null) {
            LOG.info("Stopping OcsServer Server listening for gRPC traffic on  {}", port)
            server.shutdown()
            blockUntilShutdown()
        }
    }

    /**
     * Used for unit testing
     */
    fun forceStop() {
        if (server != null) {
            LOG.info("Stopping forcefully OcsServer Server listening for gRPC traffic on  {}", port)
            server.shutdownNow()
        }
    }

    @Throws(InterruptedException::class)
    private fun blockUntilShutdown() {
        server?.awaitTermination()
    }
}
