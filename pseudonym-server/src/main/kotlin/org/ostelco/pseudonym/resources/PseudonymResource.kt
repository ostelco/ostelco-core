package org.ostelco.pseudonym.resources

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.Key
import com.google.cloud.datastore.Query
import com.google.cloud.datastore.StructuredQuery.PropertyFilter
import org.hibernate.validator.constraints.NotBlank
import org.ostelco.prime.model.ActivePseudonyms
import org.ostelco.prime.model.PseudonymEntity
import org.ostelco.pseudonym.managed.PseudonymExport
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import kotlin.collections.HashMap


const val PseudonymEntityKind = "Pseudonym"
const val msisdnPropertyName = "msisdn"
const val pseudonymPropertyName = "pseudonym"
const val startPropertyName = "start"
const val endPropertyName = "end"

/**
 * Class representing the Export task entity in Datastore.
 */
data class ExportTask(val exportId: String, val status: String, val error: String)

const val ExportTaskKind = "ExportTask"
const val exportIdPropertyName = "exportId"
const val statusPropertyName = "status"
const val errorPropertyName = "error"


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

/**
 * Resource used to handle the pseudonym related REST calls. The map of pseudonym objects
 * are store in datastore. The key for the object is made from "<msisdn>-<start timestamp ms>.
 */
@Path("/pseudonym")
class PseudonymResource(val datastore: Datastore, val dateBounds: DateBounds, val bigquery: BigQuery?) {

    private val LOG = LoggerFactory.getLogger(PseudonymResource::class.java)
    private val executor = Executors.newFixedThreadPool(3)

    /**
     * Get the pseudonym which is valid at the timestamp for the given
     * msisdn. In case pseudonym doesn't exist, a new one will be created
     * for the period. Timestamps are in UTC
     */
    @GET
    @Path("/get/{msisdn}/{timestamp}")
    fun getPseudonym(@NotBlank @PathParam("msisdn") msisdn: String,
                     @NotBlank @PathParam("timestamp") timestamp: String): Response {
        LOG.info("GET pseudonym for Msisdn = $msisdn at timestamp = $timestamp")
        val entity = getPseudonymEntityFor(msisdn, timestamp.toLong())
        return Response.ok(entity, MediaType.APPLICATION_JSON).build()
    }

    /**
     * Get the pseudonym which is valid at the time of the call for the given
     * msisdn. In case pseudonym doesn't exist, a new one will be created
     * for the period
     */
    @GET
    @Path("/current/{msisdn}")
    fun getPseudonym(@NotBlank @PathParam("msisdn") msisdn: String): Response {
        val timestamp = Instant.now().toEpochMilli()
        LOG.info("GET pseudonym for Msisdn = $msisdn at current time, timestamp = $timestamp")
        val entity = getPseudonymEntityFor(msisdn, timestamp)
        return Response.ok(entity, MediaType.APPLICATION_JSON).build()
    }

    /**
     * Get the pseudonyms valid for current & next time periods for the given
     * msisdn. In case pseudonym doesn't exist, a new one will be created
     * for the periods
     */
    @GET
    @Path("/active/{msisdn}")
    fun getActivePseudonyms(@NotBlank @PathParam("msisdn") msisdn: String): Response {
        val currentTimestamp = Instant.now().toEpochMilli()
        val nextTimestamp = dateBounds.getNextPeriodStart(currentTimestamp)
        LOG.info("GET pseudonym for Msisdn = $msisdn at timestamps = $currentTimestamp & $nextTimestamp")
        val current = getPseudonymEntityFor(msisdn, currentTimestamp)
        val next = getPseudonymEntityFor(msisdn, nextTimestamp)
        val entity = ActivePseudonyms(current, next)
        return Response.ok(entity, MediaType.APPLICATION_JSON).build()
    }

    private fun getPseudonymEntityFor(@NotBlank msisdn: String, timestamp: Long): PseudonymEntity {
        val (bounds, keyPrefix) = dateBounds.getBoundsNKeyPrefix(msisdn, timestamp)
        var entity = getPseudonymEntity(keyPrefix)
        if (entity == null) {
            entity = createPseudonym(msisdn, bounds, keyPrefix)
        }
        return entity
    }

    /**
     * Find the msisdn and other details about the given pseudonym.
     * In case pseudonym doesn't exist, it returns 404
     */
    @GET
    @Path("/find/{pseudonym}")
    fun findPseudonym(@NotBlank @PathParam("pseudonym") pseudonym: String): Response {
        LOG.info("Find details for pseudonym = $pseudonym")
        val query = Query.newEntityQueryBuilder()
                .setKind(PseudonymEntityKind)
                .setFilter(PropertyFilter.eq(pseudonymPropertyName, pseudonym))
                .setLimit(1)
                .build()
        val results = datastore.run(query)
        if (results.hasNext()) {
            val entity = results.next()
            return Response.ok(convertToPseudonymEntity(entity), MediaType.APPLICATION_JSON).build()
        }
        LOG.info("Couldn't find, pseudonym = ${pseudonym}")
        return Response.status(Status.NOT_FOUND).build()
    }

    /**
     * Delete all pseudonym entities for the given msisdn
     * Returns a json object with no of records deleted.
     *  { count : <no. of entities deleted> }
     */
    @DELETE
    @Path("/delete/{msisdn}")
    fun deleteAllPseudonyms(@NotBlank @PathParam("msisdn") msisdn: String): Response {
        LOG.info("delete all pseudonyms for Msisdn = $msisdn")
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
        // Return a Json object with number of records deleted.
        val countMap = HashMap<String, Int>()
        countMap["count"] = count
        LOG.info("deleted $count records for Msisdn = $msisdn")
        return Response.ok(countMap, MediaType.APPLICATION_JSON).build()
    }

    /**
     * Exports all pseudonyms to a bigquery table. the name is generated from the exportId
     * parameter. The exportId is expected to be a UUIDV4. The '-' are removed from the name.
     * Return Ok when the process has started, used /exportstatus to get the result.
     */
    @GET
    @Path("/export/{exportId}")
    fun exportPseudonyms(@NotBlank @PathParam("exportId") exportId: String): Response {
        LOG.info("GET export all pseudonyms to the table $exportId")
        if (bigquery == null) {
            LOG.info("BigQuery is not available, ignoring export request $exportId")
            return Response.status(Status.NOT_FOUND).build()
        }
        val exporter = PseudonymExport(exportId, bigquery, datastore)
        executor.execute(exporter.getRunnable())
        return Response.ok("Started Exporting", MediaType.TEXT_PLAIN).build()
    }

    /**
     * Returns the result of the export. Return 404 if exportId id not found in the system
     */
    @GET
    @Path("/exportstatus/{exportId}")
    fun getExportStatus(@NotBlank @PathParam("exportId") exportId: String): Response {
        LOG.info("GET status of export $exportId")
        val exportTask = getExportTask(exportId)
        if (exportTask != null) {
            return Response.ok(exportTask, MediaType.APPLICATION_JSON).build()
        }
        return Response.status(Status.NOT_FOUND).build()
    }

    private fun getExportTask(exportId: String): ExportTask? {
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

    private fun convertToPseudonymEntity(entity: Entity): PseudonymEntity {
        return PseudonymEntity(
                entity.getString(msisdnPropertyName),
                entity.getString(pseudonymPropertyName),
                entity.getLong(startPropertyName),
                entity.getLong(endPropertyName))
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
}
