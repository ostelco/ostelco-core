package org.ostelco.importer

import io.dropwizard.Application
import io.dropwizard.Configuration
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response
import io.dropwizard.jetty.HttpConnectorFactory
import io.dropwizard.server.DefaultServerFactory
import io.dropwizard.setup.Environment
import org.glassfish.jersey.client.ClientProperties
import org.slf4j.LoggerFactory
import javax.ws.rs.client.Client


/**
 * The configuration for Importer.
 */
class ImporterConfig : Configuration() {
}


/**
 * Resource used to handle the importer related REST calls.
 */
@Path("/importer")
class ImporterResource() {

    private val LOG = LoggerFactory.getLogger(ImporterResource::class.java)

    /**
     * Get the status
     */
    @GET
    @Path("/get/status")
    fun getStatus(): Response {
        LOG.info("GET status for importer")
        return Response.ok().build()
    }
}

/**
 * Entry point for running the server
 */
fun main(args: Array<String>) {
    ImporterApplication().run(*args)
}

/**
 * Dropwizard application for running pseudonymiser service that
 * converts Data-Traffic PubSub message to a pseudonymised version.
 */
class ImporterApplication : Application<ImporterConfig>() {

    private val LOG = LoggerFactory.getLogger(ImporterApplication::class.java)

    // Run the dropwizard application (called by the kotlin [main] wrapper).
    override fun run(
            config: ImporterConfig,
            env: Environment) {
        env.jersey().register(ImporterResource())
    }
}