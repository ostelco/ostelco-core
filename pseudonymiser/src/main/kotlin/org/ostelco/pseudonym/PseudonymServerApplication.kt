package org.ostelco.pseudonym

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.NoCredentials
import com.google.cloud.ServiceOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.ostelco.pseudonym.config.PseudonymServerConfig
import org.ostelco.pseudonym.resources.PseudonymResource
import io.dropwizard.Application
import io.dropwizard.setup.Environment
import org.slf4j.LoggerFactory
import java.io.FileInputStream

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.Key
import org.ostelco.pseudonym.utils.WeeklyBounds
import com.google.cloud.ServiceOptions.getNoRetrySettings



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
        val datastore = DatastoreOptions.getDefaultInstance().service
        env.jersey().register(PseudonymResource(datastore, WeeklyBounds()))
    }
}