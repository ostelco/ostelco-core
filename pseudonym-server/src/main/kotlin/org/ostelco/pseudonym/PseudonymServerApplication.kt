package org.ostelco.pseudonym

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import io.dropwizard.Application
import io.dropwizard.setup.Environment
import org.ostelco.pseudonym.config.PseudonymServerConfig
import org.ostelco.pseudonym.resources.PseudonymResource
import org.ostelco.pseudonym.utils.WeeklyBounds
import org.slf4j.LoggerFactory


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
        env.jersey().register(PseudonymResource(datastore, WeeklyBounds()))
    }
}