package org.ostelco.pseudonym.service

import com.codahale.metrics.health.HealthCheck
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
import org.ostelco.prime.logger
import org.ostelco.prime.model.ActivePseudonyms
import org.ostelco.prime.model.PseudonymEntity
import org.ostelco.prime.pseudonymizer.PseudonymizerService
import org.ostelco.pseudonym.ConfigRegistry
import org.ostelco.pseudonym.ExportTaskKind
import org.ostelco.pseudonym.MsisdnPseudonymEntityKind
import org.ostelco.pseudonym.endPropertyName
import org.ostelco.pseudonym.errorPropertyName
import org.ostelco.pseudonym.exportIdPropertyName
import org.ostelco.pseudonym.msisdnPropertyName
import org.ostelco.pseudonym.pseudonymPropertyName
import org.ostelco.pseudonym.resources.ExportTask
import org.ostelco.pseudonym.startPropertyName
import org.ostelco.pseudonym.statusPropertyName
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

    private val logger by logger()

    private lateinit var datastore: Datastore
    private var bigQuery: BigQuery? = null
    private val dateBounds: DateBounds = WeeklyBounds()

    private val executor = Executors.newFixedThreadPool(3)

    val pseudonymCache: Cache<String, PseudonymEntity> = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .build()

    fun init(env: Environment?, bq: BigQuery? = null) {

        initDatastore(env)

        bigQuery = bq ?: if (System.getenv("LOCAL_TESTING") != "true") {
            BigQueryOptions.getDefaultInstance().service
        } else {
            logger.info("Local testing, BigQuery is not available...")
            null
        }
    }

    override fun getActivePseudonymsForMsisdn(msisdn: String): ActivePseudonyms {
        val currentTimestamp = Instant.now().toEpochMilli()
        val nextTimestamp = dateBounds.getNextPeriodStart(currentTimestamp)
        logger.info("GET pseudonym for Msisdn = $msisdn at timestamps = $currentTimestamp & $nextTimestamp")
        val current = getMsisdnPseudonymEntityFor(msisdn, currentTimestamp)
        val next = getMsisdnPseudonymEntityFor(msisdn, nextTimestamp)
        return ActivePseudonyms(current, next)
    }

    override fun getMsisdnPseudonymEntityFor(msisdn: String, timestamp: Long): PseudonymEntity {
        val (bounds, keyPrefix) = dateBounds.getBoundsNKeyPrefix(msisdn, timestamp)
        // Retrieves the element from cache.
        return pseudonymCache.get(keyPrefix) {
            getMsisdnPseudonymEntity(keyPrefix) ?: createMsisdnPseudonym(msisdn, bounds, keyPrefix)
        }
    }

    fun findMsisdnPseudonym(pseudonym: String): PseudonymEntity? {
        val query = Query.newEntityQueryBuilder()
                .setKind(MsisdnPseudonymEntityKind)
                .setFilter(PropertyFilter.eq(pseudonymPropertyName, pseudonym))
                .setLimit(1)
                .build()
        val results = datastore.run(query)
        if (results.hasNext()) {
            val entity = results.next()
            return convertToMsisdnPseudonymEntity(entity)
        }
        logger.info("Couldn't find, pseudonym = $pseudonym")
        return null
    }

    fun deleteAllMsisdnPseudonyms(msisdn: String): Int {
        val query = Query.newEntityQueryBuilder()
                .setKind(MsisdnPseudonymEntityKind)
                .setFilter(PropertyFilter.eq(msisdnPropertyName, msisdn))
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

    fun exportMsisdnPseudonyms(exportId: String) {
        bigQuery?.apply {
            logger.info("GET export all pseudonyms to the table $exportId")
            val exporter = PseudonymExport(exportId = exportId, bigquery = this, datastore = datastore)
            executor.execute(exporter.getRunnable())
        }
    }

    // Integration testing helper for Datastore.
    private fun initDatastore(env: Environment?) {
        datastore = when (ConfigRegistry.config.datastoreType) {
            "inmemory-emulator" -> {
                logger.info("Starting with in-memory datastore emulator")
                val helper: LocalDatastoreHelper = LocalDatastoreHelper.create(1.0)
                helper.start()
                helper.options
            }
            "emulator" -> {
                // When prime running in GCP by hosted CI/CD, Datastore client library assumes it is running in
                // production and ignore our instruction to connect to the datastore emulator. So, we are explicitly
                // connecting to emulator
                logger.info("Connecting to datastore emulator")
                DatastoreOptions
                        .newBuilder()
                        .setHost("localhost:9090")
                        .setTransportOptions(HttpTransportOptions.newBuilder().build())
                        .build()
            }
            else -> {
                logger.info("Created default instance of datastore client")
                DatastoreOptions.getDefaultInstance()
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
        val exportKey = datastore.newKeyFactory().setKind(ExportTaskKind).newKey(exportId)
        val value = datastore.get(exportKey)
        if (value != null) {
            // Create the object from datastore entity
            return ExportTask(
                    value.getString(exportIdPropertyName),
                    value.getString(statusPropertyName),
                    value.getString(errorPropertyName))
        }
        return null
    }

    private fun getMsisdnPseudonymKey(keyPrefix: String): Key {
        return datastore.newKeyFactory().setKind(MsisdnPseudonymEntityKind).newKey(keyPrefix)
    }

    private fun getMsisdnPseudonymEntity(keyPrefix: String): PseudonymEntity? {
        val pseudonymKey = getMsisdnPseudonymKey(keyPrefix)
        val value = datastore.get(pseudonymKey)
        if (value != null) {
            // Create the object from datastore entity
            return convertToMsisdnPseudonymEntity(value)
        }
        return null
    }

    private fun createMsisdnPseudonym(msisdn: String, bounds: Bounds, keyPrefix: String): PseudonymEntity {
        val uuid = UUID.randomUUID().toString()
        var entity = PseudonymEntity(msisdn, uuid, bounds.start, bounds.end)
        val pseudonymKey = getMsisdnPseudonymKey(keyPrefix)

        val transaction = datastore.newTransaction()
        try {
            // Verify before writing a new value.
            val currentEntity = transaction.get(pseudonymKey)
            if (currentEntity == null) {
                // Prepare the new datastore entity
                val pseudonym = Entity.newBuilder(pseudonymKey)
                        .set(msisdnPropertyName, entity.sourceId)
                        .set(pseudonymPropertyName, entity.pseudonym)
                        .set(startPropertyName, entity.start)
                        .set(endPropertyName, entity.end)
                        .build()
                transaction.put(pseudonym)
                transaction.commit()
            } else {
                // Use the existing one
                entity = convertToMsisdnPseudonymEntity(currentEntity)
            }
        } finally {
            if (transaction.isActive) {
                transaction.rollback()
            }
        }
        return entity
    }

    private fun convertToMsisdnPseudonymEntity(entity: Entity): PseudonymEntity {
        return PseudonymEntity(
                entity.getString(msisdnPropertyName),
                entity.getString(pseudonymPropertyName),
                entity.getLong(startPropertyName),
                entity.getLong(endPropertyName))
    }
}
