package org.ostelco.pseudonym.service

import com.codahale.metrics.health.HealthCheck
import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.Key
import com.google.cloud.datastore.Query
import com.google.cloud.datastore.StructuredQuery.PropertyFilter
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.google.cloud.http.HttpTransportOptions
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.dropwizard.setup.Environment
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.ActivePseudonyms
import org.ostelco.prime.model.PseudonymEntity
import org.ostelco.prime.pseudonymizer.PseudonymizerService
import org.ostelco.pseudonym.ConfigRegistry
import org.ostelco.pseudonym.ExportTaskKind
import org.ostelco.pseudonym.ExportTaskKind.errorPropertyName
import org.ostelco.pseudonym.ExportTaskKind.statusPropertyName
import org.ostelco.pseudonym.PseudonymKind
import org.ostelco.pseudonym.PseudonymKindEnum.MSISDN
import org.ostelco.pseudonym.PseudonymKindEnum.SUBSCRIBER_ID
import org.ostelco.pseudonym.resources.ExportTask
import org.ostelco.pseudonym.utils.WeeklyBounds
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors

class PseudonymServiceImpl : PseudonymizerService by PseudonymizerServiceSingleton

/**
 * Class representing the boundary timestamps.
 */
data class Bounds(val start: Long, val end: Long)

/**
 * Interface which provides the method to retrieve the boundary timestamps.
 */
interface DateBounds {
    /**
     * Returns the boundaries for the period of the given timestamp.
     * (start <= timestamp <= end). Timestamps are in UTC
     * Also returns the key prefix
     */
    fun getBoundsNKeyPrefix(msisdn: String, timestamp: Long): Pair<Bounds, String>

    /**
     * Returns the timestamp for start of the next period for given timestamp.
     * (value > timestamp). Timestamps are in UTC
     */
    fun getNextPeriodStart(timestamp: Long): Long
}

object PseudonymizerServiceSingleton : PseudonymizerService {

    private val logger by getLogger()

    private lateinit var datastore: Datastore
    private var bigQuery: BigQuery? = null
    private val dateBounds: DateBounds = WeeklyBounds()

    private val msisdnPseudonymiser: Pseudonymizer = Pseudonymizer(MSISDN.kindInfo)
    private val subscriberIdPseudonymiser: Pseudonymizer = Pseudonymizer(SUBSCRIBER_ID.kindInfo)
    private val executor = Executors.newFixedThreadPool(3)

    private val msisdnPseudonymCache: Cache<String, PseudonymEntity> = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .build()
    private val subscriberIdPseudonymCache: Cache<String, PseudonymEntity> = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .build()
    // Used by unit tests
    private lateinit var localDatastoreHelper: LocalDatastoreHelper

    fun init(env: Environment?, bq: BigQuery? = null) {

        initDatastore(env)

        bigQuery = bq ?: if (System.getenv("LOCAL_TESTING") != "true") {
            BigQueryOptions.getDefaultInstance().service
        } else {
            logger.info("Local testing, BigQuery is not available...")
            null
        }
        msisdnPseudonymiser.init(datastore, bigQuery, dateBounds)
        subscriberIdPseudonymiser.init(datastore, bigQuery, dateBounds)
    }

    fun cleanup() {
        if (ConfigRegistry.config.datastoreType == "inmemory-emulator") {
            // Stop the emulator after unit tests.
            localDatastoreHelper.stop()
        }
    }

    override fun getActivePseudonymsForSubscriberId(subscriberId: String): ActivePseudonyms {
        val currentTimestamp = Instant.now().toEpochMilli()
        val nextTimestamp = dateBounds.getNextPeriodStart(currentTimestamp)
        logger.info("GET pseudonym for subscriberId = $subscriberId at timestamps = $currentTimestamp & $nextTimestamp")
        val current = getSubscriberIdPseudonym(subscriberId, currentTimestamp)
        val next = getSubscriberIdPseudonym(subscriberId, nextTimestamp)
        return ActivePseudonyms(current, next)
    }

    override fun getMsisdnPseudonym(msisdn: String, timestamp: Long): PseudonymEntity {
        val (bounds, keyPrefix) = dateBounds.getBoundsNKeyPrefix(msisdn, timestamp)
        // Retrieves the element from cache.
        return msisdnPseudonymCache.get(keyPrefix) {
            msisdnPseudonymiser.getPseudonymEntity(keyPrefix)
                    ?: msisdnPseudonymiser.createPseudonym(msisdn, bounds, keyPrefix)
        }
    }

    override fun getSubscriberIdPseudonym(subscriberId: String, timestamp: Long): PseudonymEntity {
        val (bounds, keyPrefix) = dateBounds.getBoundsNKeyPrefix(subscriberId, timestamp)
        // Retrieves the element from cache.
        return subscriberIdPseudonymCache.get(keyPrefix) {
            subscriberIdPseudonymiser.getPseudonymEntity(keyPrefix)
                    ?: subscriberIdPseudonymiser.createPseudonym(subscriberId, bounds, keyPrefix)
        }
    }

    fun findMsisdnPseudonym(pseudonym: String): PseudonymEntity? {
        return msisdnPseudonymiser.findPseudonym(pseudonym)
    }

    fun deleteAllMsisdnPseudonyms(msisdn: String): Int {
        return msisdnPseudonymiser.deleteAllPseudonyms(msisdn)
    }

    fun exportMsisdnPseudonyms(exportId: String) {
        bigQuery?.apply {
            logger.info("GET export all pseudonyms to the table $exportId")
            val exporter = PseudonymExport(exportId = exportId, bigQuery = this, datastore = datastore)
            executor.execute(exporter.getRunnable())
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

    fun getExportTask(exportId: String): ExportTask? {
        val exportKey = datastore.newKeyFactory().setKind(ExportTaskKind.kindName).newKey(exportId)
        val value = datastore.get(exportKey)
        if (value != null) {
            // Create the object from datastore entity
            return ExportTask(
                    value.getString(ExportTaskKind.idPropertyName),
                    value.getString(statusPropertyName),
                    value.getString(errorPropertyName))
        }
        return null
    }
}


class Pseudonymizer(private val pseudonymKind: PseudonymKind) {
    private val logger by getLogger()
    private lateinit var datastore: Datastore
    private var bigQuery: BigQuery? = null
    private lateinit var dateBounds: DateBounds

    fun init(ds: Datastore, bq: BigQuery? = null, bounds: DateBounds) {
        datastore = ds
        bigQuery = bq
        dateBounds = bounds
    }

    fun findPseudonym(pseudonym: String): PseudonymEntity? {
        val query = Query.newEntityQueryBuilder()
                .setKind(pseudonymKind.kindName)
                .setFilter(PropertyFilter.eq(pseudonymKind.pseudonymPropertyName, pseudonym))
                .setLimit(1)
                .build()
        val results = datastore.run(query)
        if (results.hasNext()) {
            val entity = results.next()
            return convertToPseudonymEntity(entity)
        }
        logger.info("Couldn't find, pseudonym = $pseudonym")
        return null
    }

    fun deleteAllPseudonyms(sourceId: String): Int {
        val query = Query.newEntityQueryBuilder()
                .setKind(pseudonymKind.kindName)
                .setFilter(PropertyFilter.eq(pseudonymKind.idPropertyName, sourceId))
                .setLimit(1)
                .build()
        val results = datastore.run(query)
        var count = 0
        while (results.hasNext()) {
            val entity = results.next()
            datastore.delete(entity.key)
            count++
        }
        return count
    }

    private fun getPseudonymKey(keyPrefix: String): Key {
        return datastore.newKeyFactory().setKind(pseudonymKind.kindName).newKey(keyPrefix)
    }

    fun getPseudonymEntity(keyPrefix: String): PseudonymEntity? {
        val pseudonymKey = getPseudonymKey(keyPrefix)
        val value = datastore.get(pseudonymKey)
        if (value != null) {
            // Create the object from datastore entity
            return convertToPseudonymEntity(value)
        }
        return null
    }

    fun createPseudonym(sourceId: String, bounds: Bounds, keyPrefix: String): PseudonymEntity {
        val uuid = UUID.randomUUID().toString()
        var entity = PseudonymEntity(sourceId, uuid, bounds.start, bounds.end)
        val pseudonymKey = getPseudonymKey(keyPrefix)

        val transaction = datastore.newTransaction()
        try {
            // Verify before writing a new value.
            val currentEntity = transaction.get(pseudonymKey)
            if (currentEntity == null) {
                // Prepare the new datastore entity
                val pseudonym = Entity.newBuilder(pseudonymKey)
                        .set(pseudonymKind.idPropertyName, entity.sourceId)
                        .set(pseudonymKind.pseudonymPropertyName, entity.pseudonym)
                        .set(pseudonymKind.startPropertyName, entity.start)
                        .set(pseudonymKind.endPropertyName, entity.end)
                        .build()
                transaction.put(pseudonym)
                transaction.commit()
            } else {
                // Use the existing one
                entity = convertToPseudonymEntity(currentEntity)
            }
        } finally {
            if (transaction.isActive) {
                transaction.rollback()
            }
        }
        return entity
    }

    private fun convertToPseudonymEntity(entity: Entity): PseudonymEntity {
        return PseudonymEntity(
                sourceId = entity.getString(pseudonymKind.idPropertyName),
                pseudonym = entity.getString(pseudonymKind.pseudonymPropertyName),
                start = entity.getLong(pseudonymKind.startPropertyName),
                end = entity.getLong(pseudonymKind.endPropertyName))
    }
}
