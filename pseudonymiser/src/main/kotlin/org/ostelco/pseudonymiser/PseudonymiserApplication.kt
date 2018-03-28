package org.ostelco.pseudonymiser

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import io.dropwizard.Application
import io.dropwizard.setup.Environment
import org.ostelco.pseudonymiser.config.PseudonymiserConfig
import org.ostelco.pseudonymiser.resources.PseudonymiserResource
import org.slf4j.LoggerFactory


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
        env.jersey().register(PseudonymiserResource())
    }
}