package org.ostelco.exporter

import io.dropwizard.Application
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.jetty.HttpConnectorFactory
import io.dropwizard.server.DefaultServerFactory
import io.dropwizard.setup.Environment
import org.glassfish.jersey.client.ClientProperties
import org.ostelco.exporter.config.ExporterConfig
import org.ostelco.exporter.managed.MessageProcessor
import org.ostelco.exporter.resources.ExporterResource
import org.slf4j.LoggerFactory
import javax.ws.rs.client.Client


/**
 * Entry point for running the server
 */
fun main(args: Array<String>) {
    ExporterApplication().run(*args)
}

/**
 * Dropwizard application for running pseudonymiser service that
 * converts Data-Traffic PubSub message to a pseudonymised version.
 */
class ExporterApplication : Application<ExporterConfig>() {

    private val LOG = LoggerFactory.getLogger(ExporterApplication::class.java)

    // Find port for the local REST endpoint
    private fun getPseudonymEndpoint(config: ExporterConfig): String {
        var endpoint = config.pseudonymEndpoint
        if (!endpoint.isEmpty()) {
            return endpoint
        }
        var httpPort: Int? = null
        val serverFactory = config.getServerFactory() as? DefaultServerFactory
        if (serverFactory != null) {
            for (connector in serverFactory.applicationConnectors) {
                if (connector.javaClass.isAssignableFrom(HttpConnectorFactory::class.java)) {
                    httpPort = (connector as? HttpConnectorFactory)?.port
                    break
                }
            }
        }
        return "http://localhost:${httpPort?:8080}"
    }

    // Run the dropwizard application (called by the kotlin [main] wrapper).
    override fun run(
            config: ExporterConfig,
            env: Environment) {
        val client: Client = JerseyClientBuilder(env).using(config.jerseyClient).build(name);
        // Increase HTTP timeout values
        client.property(ClientProperties.CONNECT_TIMEOUT, 2000)
        client.property(ClientProperties.READ_TIMEOUT, 2000)
        val endpoint = getPseudonymEndpoint(config)
        LOG.info("Pseudonym endpoint = $endpoint")
        val messageProcessor = MessageProcessor(endpoint, client)
        env.lifecycle().manage(messageProcessor)
        env.jersey().register(ExporterResource())
    }
}