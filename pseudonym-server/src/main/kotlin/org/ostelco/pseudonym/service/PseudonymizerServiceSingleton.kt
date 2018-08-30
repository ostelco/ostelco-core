package org.ostelco.pseudonym.service

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.Key
import com.google.cloud.datastore.Query
import com.google.cloud.datastore.StructuredQuery.PropertyFilter
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import org.hibernate.validator.constraints.NotBlank
import org.ostelco.prime.logger
import org.ostelco.prime.model.ActivePseudonyms
import org.ostelco.prime.model.PseudonymEntity
import org.ostelco.prime.pseudonymizer.PseudonymizerService
import org.ostelco.pseudonym.ConfigRegistry
import org.ostelco.pseudonym.ExportTaskKind
import org.ostelco.pseudonym.PseudonymEntityKind
import org.ostelco.pseudonym.PseudonymServerConfig
import org.ostelco.pseudonym.endPropertyName
import org.ostelco.pseudonym.errorPropertyName
import org.ostelco.pseudonym.exportIdPropertyName
import org.ostelco.pseudonym.managed.PseudonymExport
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
    private lateinit var bigquery: BigQuery
    private val dateBounds: DateBounds = WeeklyBounds()

    private val executor = Executors.newFixedThreadPool(3)

    fun init(bq: BigQuery? = null) {
        datastore = getDatastore(ConfigRegistry.config)
        if (bq != null) {
            bigquery = bq
        } else {
            if (System.getenv("LOCAL_TESTING") != "true") {
                bigquery = BigQueryOptions.getDefaultInstance().service
            } else {
                logger.info("Local testing, BigQuery is not available...")
            }
        }
    }

    override fun getActivePseudonymsForMsisdn(msisdn: String): ActivePseudonyms {
        val currentTimestamp = Instant.now().toEpochMilli()
        val nextTimestamp = dateBounds.getNextPeriodStart(currentTimestamp)
        logger.info("GET pseudonym for Msisdn = $msisdn at timestamps = $currentTimestamp & $nextTimestamp")
        val current = getPseudonymEntityFor(msisdn, currentTimestamp)
        val next = getPseudonymEntityFor(msisdn, nextTimestamp)
        return ActivePseudonyms(current, next)
    }

    fun findPseudonym(pseudonym: String): PseudonymEntity? {
        val query = Query.newEntityQueryBuilder()
                .setKind(PseudonymEntityKind)
                .setFilter(PropertyFilter.eq(pseudonymPropertyName, pseudonym))
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

    fun deleteAllPseudonyms(msisdn: String): Int {
        val query = Query.newEntityQueryBuilder()
                .setKind(PseudonymEntityKind)
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

    fun exportPseudonyms(exportId: String) {
        logger.info("GET export all pseudonyms to the table $exportId")
        val exporter = PseudonymExport(exportId, bigquery, datastore)
        executor.execute(exporter.getRunnable())
    }

    // Integration testing helper for Datastore.
    private fun getDatastore(config: PseudonymServerConfig): Datastore {
        val datastore: Datastore?
        if (config.datastoreType == "inmemory-emulator") {
            logger.info("Starting with in-memory datastore emulator...")
            val helper: LocalDatastoreHelper = LocalDatastoreHelper.create(1.0)
            helper.start()
            datastore = helper.options.service
        } else {
            datastore = DatastoreOptions.getDefaultInstance().service
            logger.info("Created default instance of datastore client")

            // TODO vihang: make this part of health-check
            val testKey = datastore.newKeyFactory().setKind("TestKind").newKey("testKey")
            val testPropertyKey = "testPropertyKey"
            val testPropertyValue = "testPropertyValue"
            val testEntity = Entity.newBuilder(testKey).set(testPropertyKey, testPropertyValue).build()
            datastore.put(testEntity)
            val value = datastore.get(testKey).getString(testPropertyKey)
            if (testPropertyValue != value) {
                logger.warn("Unable to fetch test property value from datastore")
            }
            datastore.delete(testKey)
            // END
        }
        return datastore
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

    private fun getPseudonymKey(keyPrefix: String): Key {
        return datastore.newKeyFactory().setKind(PseudonymEntityKind).newKey(keyPrefix)
    }

    private fun getPseudonymEntity(keyPrefix: String): PseudonymEntity? {
        val pseudonymKey = getPseudonymKey(keyPrefix)
        val value = datastore.get(pseudonymKey)
        if (value != null) {
            // Create the object from datastore entity
            return convertToPseudonymEntity(value)
        }
        return null
    }

    fun getPseudonymEntityFor(@NotBlank msisdn: String, timestamp: Long): PseudonymEntity {
        val (bounds, keyPrefix) = dateBounds.getBoundsNKeyPrefix(msisdn, timestamp)
        var entity = getPseudonymEntity(keyPrefix)
        if (entity == null) {
            entity = createPseudonym(msisdn, bounds, keyPrefix)
        }
        return entity
    }

    private fun createPseudonym(msisdn: String, bounds: Bounds, keyPrefix: String): PseudonymEntity {
        val uuid = UUID.randomUUID().toString()
        var entity = PseudonymEntity(msisdn, uuid, bounds.start, bounds.end)
        val pseudonymKey = getPseudonymKey(keyPrefix)

        val transaction = datastore.newTransaction()
        try {
            // Verify before writing a new value.
            val currentEntity = transaction.get(pseudonymKey)
            if (currentEntity == null) {
                // Prepare the new datastore entity
                val pseudonym = Entity.newBuilder(pseudonymKey)
                        .set(msisdnPropertyName, entity.msisdn)
                        .set(pseudonymPropertyName, entity.pseudonym)
                        .set(startPropertyName, entity.start)
                        .set(endPropertyName, entity.end)
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
                entity.getString(msisdnPropertyName),
                entity.getString(pseudonymPropertyName),
                entity.getLong(startPropertyName),
                entity.getLong(endPropertyName))
    }
}