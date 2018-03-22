package org.ostelco.pseudonym

import com.google.auth.oauth2.GoogleCredentials
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

        val datastore = DatastoreOptions.getDefaultInstance().getService();
//        // The kind for the new entity
//        val kind = "Task"
//        // The name/ID for the new entity
//        val name = "sampletask1"
//        // The Cloud Datastore key for the new entity
//        val taskKey = datastore.newKeyFactory().setKind(kind).newKey(name)
//
//        // Prepares the new entity
//        val task = Entity.newBuilder(taskKey)
//                .set("description", "Buy milk")
//                .build()
//
//        // Saves the entity
//        datastore.put(task)
//
//        System.out.printf("Saved %s: %s%n", task.key.name, task.getString("description"))
//
//        //Retrieve entity
//        val retrieved = datastore.get(taskKey)
//
//        System.out.printf("Retrieved %s: %s%n", taskKey.name, retrieved.getString("description"))

        env.jersey().register(PseudonymResource(datastore, WeeklyBounds()))
    }
}