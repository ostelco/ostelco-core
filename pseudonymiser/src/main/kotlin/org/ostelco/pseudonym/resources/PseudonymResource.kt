package org.ostelco.pseudonym.resources

import com.google.cloud.datastore.*
import com.google.cloud.datastore.StructuredQuery.PropertyFilter
import org.hibernate.validator.constraints.NotBlank
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import java.util.TimeZone


/**
 * Interface which provides the method to retrieve the boundary timestamps.
 */
interface DateBounds {
    /**
     * Returns the boundaries for the period of the given timestamp.
     * (start <= timestamp <= end). Timestamps are in UTC
     */
    fun getBounds(timestamp: Long): Pair<Long, Long>
}

/**
 * Class representing the Pseudonym entity in Datastore.
 */
class PseudonymEntity(val msisdn: String, val pseudonym: String, val start: Long, val end: Long)

/**
 * Resource used to handle the pseudonym related REST calls. The map of pseudonym objects
 * are store in datastore. The key for the object is made from "<msisdn>-<start timestamp ms>.
 */
@Path("/pseudonym")
class PseudonymResource(val datastore: Datastore, val dateBounds: DateBounds) {

    private val LOG = LoggerFactory.getLogger(PseudonymResource::class.java)
    private val timeZone = TimeZone.getTimeZone("UTC")
    private val dataType = "Pseudonym"
    /**
     * Get the pseudonym which is valid at the timestamp for the given
     * msisdn. In case pseudonym doesn't exist, a new one will be created
     * for the period. Timestamps are in UTC
     */
    @GET
    @Path("/get/{msisdn}/{timestamp}")
    fun getPseudonym(@NotBlank @PathParam("msisdn") msisdn: String,
                     @NotBlank @PathParam("timestamp") timestamp: String): Response {
        LOG.info("pseudonym for Msisdn = ${msisdn} at timestamp =${timestamp}")
        val bounds = dateBounds.getBounds(timestamp.toLong())
        var entity = getPseudonymEntity(msisdn, bounds.first)
        if (entity == null) {
            entity = createPseudonym(msisdn, bounds)
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
        LOG.info("pseudonym for Msisdn = ${msisdn} at current time, timestamp =${timestamp}")
        val bounds = dateBounds.getBounds(timestamp)
        var entity = getPseudonymEntity(msisdn, bounds.first)
        if (entity == null) {
            entity = createPseudonym(msisdn, bounds)
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
        LOG.info("Find details for pseudonym = ${pseudonym}")
        val query = Query.newEntityQueryBuilder()
                .setKind(dataType)
                .setFilter(PropertyFilter.eq("pseudonym", pseudonym))
                .setLimit(1)
                .build()
        val results = datastore.run(query)
        if (results.hasNext()) {
            val entity = results.next()
            return Response.ok(entity, MediaType.APPLICATION_JSON).build()
        }
        LOG.info("Couldn't find, pseudonym = ${pseudonym}")
        return Response.status(Status.NOT_FOUND).build()
    }

    private fun getPseudonymKey(msisdn: String, start: Long): Key {
        val keyName = "${msisdn}-${start}"
        return datastore.newKeyFactory().setKind(dataType).newKey(keyName)
    }

    private fun getPseudonymEntity(msisdn: String, start: Long): PseudonymEntity? {
        val pseudonymKey = getPseudonymKey(msisdn, start)
        val value = datastore.get(pseudonymKey)
        if (value != null) {
            // Create the object from datastore entity
            return convertToPseudonymEntity(value)
        }
        return null
    }

    private fun convertToPseudonymEntity(entity: Entity): PseudonymEntity {
        return PseudonymEntity(
                entity.getString("msisdn"),
                entity.getString("pseudonym"),
                entity.getLong("start"),
                entity.getLong("end"))
    }

    private fun createPseudonym(msisdn: String, bounds: Pair<Long, Long>): PseudonymEntity {
        val uuid = UUID.randomUUID().toString();
        var entity = PseudonymEntity(msisdn, uuid, bounds.first, bounds.second)
        val pseudonymKey = getPseudonymKey(entity.msisdn, entity.start)

        val transaction = datastore.newTransaction()
        try {
            // Verify before writing a new value.
            val currentEntity = transaction.get(pseudonymKey);
            if (currentEntity == null) {
                // Prepare the new datastore entity
                val pseudonym = Entity.newBuilder(pseudonymKey)
                        .set("msisdn", entity.msisdn)
                        .set("pseudonym", entity.pseudonym)
                        .set("start", entity.start)
                        .set("end", entity.end)
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
