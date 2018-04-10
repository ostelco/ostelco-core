package org.ostelco.exporter.managed

import io.dropwizard.lifecycle.Managed
import org.slf4j.LoggerFactory
import javax.ws.rs.client.Client


/**
 * This calss converts the Plain DataTrafficInfo message to
 * a pseudonymised version. Pushes the new message
 * to different PubSub topic.
 */

class MessageProcessor(private val pseudonymEndpoint: String,
                       private val client: Client) : Managed {

    private val LOG = LoggerFactory.getLogger(MessageProcessor::class.java)

    @Throws(Exception::class)
    override fun start() {
        LOG.info("Starting MessageProcessor...")
    }

    @Throws(Exception::class)
    override fun stop() {
        LOG.info("Stopping MessageProcessor...")
    }

}

