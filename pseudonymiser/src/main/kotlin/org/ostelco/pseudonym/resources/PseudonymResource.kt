package org.ostelco.pseudonym.resources

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.Key
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
    fun getBounds(timestamp: Long): Pair<Long, Long>
}

/**
 * Class representing the Pseudonym entity in Datastore.
 */
class PseudonymEntity(val msisdn: String, val pseudonym: String, val start: Long, val end: Long)

/**
 * Resource used to handle the pseudonym related REST calls.
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
     * Generates the key for a pseudonym entity. The configurable portion
     * of the key is "<msisdn>-<start timestamp ms>".
     */
    private fun getPseudonymKey(msisdn: String, start: Long): Key {
        val keyName = "${msisdn}-${start}"
        return datastore.newKeyFactory().setKind(dataType).newKey(keyName)
    }

    /**
     * Retrieves the pseudonym for the given msisdn with starting timestamp.
     * If the entity is not found in the datastore , it returns null.
     */
    private fun getPseudonymEntity(msisdn: String, start: Long): PseudonymEntity? {
        val pseudonymKey = getPseudonymKey(msisdn, start)
        val value = datastore.get(pseudonymKey)
        if (value != null) {
            // Create the object from datastore entity
            return PseudonymEntity(
                    value.getString("msisdn"),
                    value.getString("pseudonym"),
                    value.getLong("start"),
                    value.getLong("end"))
        }
        return null
    }

    /**
     * Create a new pseudonym entity for a msisdn with given bounds.
     * This won't check for duplicates.
     */
    private fun createPseudonym(msisdn: String, bounds: Pair<Long, Long>): PseudonymEntity {
        val uuid = UUID.randomUUID().toString();
        val entity = PseudonymEntity(msisdn, uuid, bounds.first, bounds.second)
        val pseudonymKey = getPseudonymKey(entity.msisdn, entity.start)

        // Prepare the new datastore entity
        val pseudonym = Entity.newBuilder(pseudonymKey)
                .set("msisdn", entity.msisdn)
                .set("pseudonym", entity.pseudonym)
                .set("start", entity.start)
                .set("end", entity.end)
                .build()

        datastore.put(pseudonym)
        return entity
    }
}
