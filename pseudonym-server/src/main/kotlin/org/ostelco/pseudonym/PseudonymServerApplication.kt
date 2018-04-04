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
 * Entry point for running the authentiation server application
 */
fun main(args: Array<String>) {
    PseudonymServerApplication().run(*args)
}

/**
 * A Dropwizard application for running an authentication service that
 * uses Firebase to authenticate users.
 */
class PseudonymServerApplication : Application<PseudonymServerConfig>() {

    private val LOG = LoggerFactory.getLogger(PseudonymServerApplication::class.java)

    /**
     * Run the dropwizard application (called by the kotlin [main] wrapper).
     */
    override fun run(
            config: PseudonymServerConfig,
            env: Environment) {
        var datastore :Datastore?
        if (config.datastoreType == "emulator") {
            LOG.info("Starting local datastore emulator...")
            val helper: LocalDatastoreHelper = LocalDatastoreHelper.create(1.0)
            helper.start()
            datastore = helper.options.service
        } else {
            datastore = DatastoreOptions.getDefaultInstance().service
        }
        val client: Client = JerseyClientBuilder(env).using(config.jerseyClient).build(name);
        client.property(ClientProperties.CONNECT_TIMEOUT, 2000)
        client.property(ClientProperties.READ_TIMEOUT, 2000)
        val subscriptionName = ProjectSubscriptionName.of(config.projectName, config.subscriptionName)
        val publisherTopicName = ProjectTopicName.of(config.projectName, config.publisherTopic)
        var endpoint = config.pseudonymEndpoint
        if (endpoint.isEmpty()) {
            var httpPort: Int? = null
            val serverFactory = config.getServerFactory() as DefaultServerFactory
            for (connector in serverFactory.applicationConnectors) {
                if (connector.javaClass.isAssignableFrom(HttpConnectorFactory::class.java)) {
                    httpPort = (connector as? HttpConnectorFactory)?.port
                    break
                }
            }
            endpoint = "http://localhost:${httpPort?:8080}"
        }
        LOG.info("Pseudonym endpoint = $endpoint")
        val messageProcessor = MessageProcessor(subscriptionName,
                publisherTopicName,
                endpoint,
                client)
        env.lifecycle().manage(messageProcessor)
        env.jersey().register(PseudonymResource(datastore, WeeklyBounds()))
    }
}