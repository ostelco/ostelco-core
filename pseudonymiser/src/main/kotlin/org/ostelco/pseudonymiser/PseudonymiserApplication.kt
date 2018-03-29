package org.ostelco.pseudonymiser

import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import io.dropwizard.Application
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.setup.Environment
import org.ostelco.pseudonymiser.config.PseudonymiserConfig
import org.ostelco.pseudonymiser.manager.MessageProcessor
import org.ostelco.pseudonymiser.resources.PseudonymiserResource
import org.slf4j.LoggerFactory
import javax.ws.rs.client.Client


/**
 * Entry point for running the authentiation server application
 */
fun main(args: Array<String>) {
    PseudonymiserApplication().run(*args)
}

/**
 * A Dropwizard application for running an authentication service that
 * uses Firebase to authenticate users.
 */
class PseudonymiserApplication : Application<PseudonymiserConfig>() {

    private val LOG = LoggerFactory.getLogger(PseudonymiserApplication::class.java)

    /**
     * Run the dropwizard application (called by the kotlin [main] wrapper).
     */
    override fun run(
            config: PseudonymiserConfig,
            env: Environment) {

        val client: Client = JerseyClientBuilder(env).using(config.jerseyClient)
                .build(name);
        val subscriptionName = ProjectSubscriptionName.of(config.projectName, config.subscriptionName)
        val publisherTopicName = ProjectTopicName.of(config.projectName, config.publisherTopic)
        val messageProcessor = MessageProcessor(subscriptionName,
                publisherTopicName,
                config.pseudonymEndpoint,
                client)
        env.lifecycle().manage(messageProcessor)
        env.jersey().register(PseudonymiserResource())
    }
}