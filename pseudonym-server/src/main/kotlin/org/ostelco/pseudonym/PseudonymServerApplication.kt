package org.ostelco.pseudonym

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import io.dropwizard.Application
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.jetty.HttpConnectorFactory
import io.dropwizard.server.DefaultServerFactory
import io.dropwizard.setup.Environment
import org.glassfish.jersey.client.ClientProperties
import org.ostelco.pseudonym.config.PseudonymServerConfig
import org.ostelco.pseudonym.managed.MessageProcessor
import org.ostelco.pseudonym.resources.PseudonymResource
import org.ostelco.pseudonym.utils.WeeklyBounds
import org.slf4j.LoggerFactory
import javax.ws.rs.client.Client


/**
 * Entry point for running the server
 */
fun main(args: Array<String>) {
    PseudonymServerApplication().run(*args)
}

/**
 * Dropwizard application for running pseudonymiser service that
 * converts Data-Traffic PubSub message to a pseudonymised version.
 */
class PseudonymServerApplication : Application<PseudonymServerConfig>() {

    private val LOG = LoggerFactory.getLogger(PseudonymServerApplication::class.java)

    // Find port for the local REST endpoint
    fun getPseudonymEndpoint(config: PseudonymServerConfig): String {
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

    // Integration testing helper for Datastore.
    fun getDatastore(config: PseudonymServerConfig): Datastore {
        var datastore :Datastore?
        if (config.datastoreType == "inmemory-emulator") {
            LOG.info("Starting with in-memory datastore emulator...")
            val helper: LocalDatastoreHelper = LocalDatastoreHelper.create(1.0)
            helper.start()
            datastore = helper.options.service
        } else {
            datastore = DatastoreOptions.getDefaultInstance().service
        }
        return datastore
    }

    // Run the dropwizard application (called by the kotlin [main] wrapper).
    override fun run(
            config: PseudonymServerConfig,
            env: Environment) {
        val datastore = getDatastore(config)
        val client: Client = JerseyClientBuilder(env).using(config.jerseyClient).build(name);
        // Increase HTTP timeout values
        client.property(ClientProperties.CONNECT_TIMEOUT, 2000)
        client.property(ClientProperties.READ_TIMEOUT, 2000)
        val subscriptionName = ProjectSubscriptionName.of(config.projectName, config.subscriptionName)
        val publisherTopicName = ProjectTopicName.of(config.projectName, config.publisherTopic)
        val endpoint = getPseudonymEndpoint(config)
        LOG.info("Pseudonym endpoint = $endpoint")
        val messageProcessor = MessageProcessor(subscriptionName,
                publisherTopicName,
                endpoint,
                client)
        env.lifecycle().manage(messageProcessor)
        env.jersey().register(PseudonymResource(datastore, WeeklyBounds()))
    }
}