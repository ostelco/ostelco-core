package org.ostelco.prime.storage.scaninfo

import arrow.core.Either
import com.codahale.metrics.health.HealthCheck
import com.google.cloud.NoCredentials
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.google.cloud.http.HttpTransportOptions
import io.dropwizard.setup.Environment
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.*
import org.ostelco.prime.storage.*


class ScanInfoStore : ScanInformationStore by ScanInformationStoreSingleton

object ScanInformationStoreSingleton : ScanInformationStore {

    private val logger by getLogger()
    private lateinit var datastore: Datastore
    // Used by unit tests
    private lateinit var localDatastoreHelper: LocalDatastoreHelper

    override fun upsertVendorScanInformation(subscriberId: String, vendorScanInformation: VendorScanInformation): Either<StoreError, Unit> {
        val testKey = datastore.newKeyFactory().setKind("TestKind").newKey("testKey")
        val testPropertyKey = "testPropertyKey"
        val testPropertyValue = "testPropertyValue"
        val testEntity = Entity.newBuilder(testKey).set(testPropertyKey, testPropertyValue).build()
        datastore.put(testEntity)
        val value = datastore.get(testKey).getString(testPropertyKey)
        datastore.delete(testKey)
        return Either.right(Unit)
    }

    fun init(env: Environment?) {
        initDatastore(env)
    }

    fun cleanup() {
        if (ConfigRegistry.config.datastoreType == "inmemory-emulator") {
            // Stop the emulator after unit tests.
            logger.info("Stopping in-memory datastore emulator")
            localDatastoreHelper.stop()
        }
    }

    // Integration testing helper for Datastore.
    private fun initDatastore(env: Environment?) {
        datastore = when (ConfigRegistry.config.datastoreType) {
            "inmemory-emulator" -> {
                logger.info("Starting with in-memory datastore emulator")
                localDatastoreHelper = LocalDatastoreHelper.create(1.0)
                localDatastoreHelper.start()
                localDatastoreHelper.options
            }
            "emulator" -> {
                // When prime running in GCP by hosted CI/CD, Datastore client library assumes it is running in
                // production and ignore our instruction to connect to the datastore emulator. So, we are explicitly
                // connecting to emulator
                logger.info("Connecting to datastore emulator")
                DatastoreOptions
                        .newBuilder()
                        .setHost("localhost:9090")
                        .setCredentials(NoCredentials.getInstance())
                        .setTransportOptions(HttpTransportOptions.newBuilder().build())
                        .build()
            }
            else -> {
                logger.info("Created default instance of datastore client")
                DatastoreOptions
                        .newBuilder()
                        .setNamespace(ConfigRegistry.config.namespace)
                        .build()
            }
        }.service

        // health-check for datastore
        env?.healthChecks()?.register("datastore", object : HealthCheck() {
            override fun check(): Result {
                try {
                    val testKey = datastore.newKeyFactory().setKind("TestKind").newKey("testKey")
                    val testPropertyKey = "testPropertyKey"
                    val testPropertyValue = "testPropertyValue"
                    val testEntity = Entity.newBuilder(testKey).set(testPropertyKey, testPropertyValue).build()
                    datastore.put(testEntity)
                    val value = datastore.get(testKey).getString(testPropertyKey)
                    datastore.delete(testKey)
                    if (testPropertyValue != value) {
                        logger.warn("Unable to fetch test property value from datastore")
                        return Result.builder().unhealthy().build()
                    }
                    return Result.builder().healthy().build()
                } catch (e: Exception) {
                    return Result.builder().unhealthy(e).build()
                }
            }
        })
    }
}
