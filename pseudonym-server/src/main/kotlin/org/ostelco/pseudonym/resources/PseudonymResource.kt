package org.ostelco.pseudonym.resources

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.Key
import com.google.cloud.datastore.Query
import com.google.cloud.datastore.StructuredQuery.PropertyFilter
import org.hibernate.validator.constraints.NotBlank
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import kotlin.collections.HashMap


/**
 * Interface which provides the method to retrieve the boundary timestamps.
 */
data class Bounds(val start: Long, val end: Long)
interface DateBounds {
    /**
     * Returns the boundaries for the period of the given timestamp.
     * (start <= timestamp <= end). Timestamps are in UTC
     * Also returns the key prefix
     */
    fun getBoundsNKeyPrefix(msisdn: String, timestamp: Long): Pair<Bounds, String>

}

/**
 * Class representing the Pseudonym entity in Datastore.
 */
data class PseudonymEntity(val msisdn: String, val pseudonym: String, val start: Long, val end: Long)

/**
 * Resource used to handle the pseudonym related REST calls. The map of pseudonym objects
 * are store in datastore. The key for the object is made from "<msisdn>-<start timestamp ms>.
 */
@Path("/pseudonym")
class PseudonymResource(val datastore: Datastore, val dateBounds: DateBounds) {

    private val LOG = LoggerFactory.getLogger(PseudonymResource::class.java)
    private val dataType = "Pseudonym"
    private val msisdnPropertyName = "msisdn"
    private val pseudonymPropertyName = "pseudonym"
    private val startPropertyName = "start"
    private val endPropertyName = "end"
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
        val (bounds, keyPrefix) = dateBounds.getBoundsNKeyPrefix(msisdn, timestamp.toLong())
        var entity = getPseudonymEntity(keyPrefix)
        if (entity == null) {
            entity = createPseudonym(msisdn, bounds, keyPrefix)
        }
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
        val (bounds, keyPrefix) = dateBounds.getBoundsNKeyPrefix(msisdn, timestamp)
        var entity = getPseudonymEntity(keyPrefix)
        if (entity == null) {
            entity = createPseudonym(msisdn, bounds, keyPrefix)
        }
        return Response.ok(entity, MediaType.APPLICATION_JSON).build()
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
                .setKind(dataType)
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
                .setKind(dataType)
                .setFilter(PropertyFilter.eq(msisdnPropertyName, msisdn))
                .setLimit(1)
                .build()
        val results = datastore.run(query)
        var count = 0;
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

    private fun getPseudonymKey(keyPrefix: String): Key {
        return datastore.newKeyFactory().setKind(dataType).newKey(keyPrefix)
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
        val uuid = UUID.randomUUID().toString();
        var entity = PseudonymEntity(msisdn, uuid, bounds.start, bounds.end)
        val pseudonymKey = getPseudonymKey(keyPrefix)

        val transaction = datastore.newTransaction()
        try {
            // Verify before writing a new value.
            val currentEntity = transaction.get(pseudonymKey);
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
