package org.ostelco.simcards.adapter

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.getLogger
import org.ostelco.simcards.admin.HssConfig
import org.ostelco.simcards.inventory.HssState
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


// TODO:
//  1. Create a simplified HLR adapter interface [done]
//  2. Extend simplifie HLR adapter to have return types that can convey error situations.
//  3. Extend simplified HLR adapter to have a liveness/health test. [done]
//  4. Refactor all other code to live with this  simplified type of hlr adapter.




/**
 * An adapter that connects to a HLR and activates/deactivates individual
 * SIM profiles.  This is a datum that is stored in a database.
 *
 * When a VLR asks the HLR for the an authentication triplet, then the
 * HLR will know that it should give an answer.
 *
 * id - is a database internal identifier.
 * name - is an unique instance of  HLR reference.
 */
data class HssEntry(
        @JsonProperty("id") val id: Long,
        @JsonProperty("name") val name: String)

/**
 * This is an interface that abstracts interactions with HSS (Home Subscriber Service)
 * implementations.
 */
interface HssAdapter {
    fun activate(simEntry: SimEntry): SimEntry?
    fun suspend(simEntry: SimEntry): SimEntry?
    fun reactivate(simEntry: SimEntry): SimEntry?
    fun terminate(simEntry: SimEntry): SimEntry?

    fun getName(): String = "Unknown HSS adapter" // XXX Don't do defaults eventually
    fun iAmHealthy(): Boolean = true
}


class Wg2HssAdapter(val httpClient: CloseableHttpClient,
                    val config: HssConfig,
                    val dao: SimInventoryDAO) : HssAdapter {

    private val logger by getLogger()

    /* For payload serializing. */
    private val mapper = jacksonObjectMapper()

    override fun reactivate(simEntry: SimEntry): SimEntry? {
        return null
    }

    override fun terminate(simEntry: SimEntry): SimEntry? {
        return null
    }



    /**
     * Requests the external HLR service to activate the SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    override fun activate(simEntry: SimEntry): SimEntry? {

        if (simEntry.iccid.isEmpty()) {
            throw WebApplicationException(
                    String.format("Illegal parameter in SIM activation request to BSSID %s",
                            config.name),
                    Response.Status.BAD_REQUEST)
        }

        val body = mapOf(
                "bssid" to config.name,
                "iccid" to simEntry.iccid,
                "msisdn" to simEntry.msisdn,
                "userid" to config.userId
        )

        val payload = mapper.writeValueAsString(body)

        val request = RequestBuilder.post()
                .setUri("${config.endpoint}/activate")
                .setHeader("x-api-key", config.apiKey)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setEntity(StringEntity(payload))
                .build()

        return httpClient.execute(request).use {
            when (it.statusLine.statusCode) {
                201 -> {
                    logger.info("HLR activation message to BSSID {} for ICCID {} completed OK",
                            config.name,
                            simEntry.iccid)
                    dao.setHlrState(simEntry.id!!, HssState.ACTIVATED)
                }
                else -> {
                    logger.warn("HLR activation message to BSSID {} for ICCID {} failed with status ({}) {}",
                            config.name,
                            simEntry.iccid,
                            it.statusLine.statusCode,
                            it.statusLine.reasonPhrase)
                    throw WebApplicationException(
                            String.format("Failed to activate ICCID %s with BSSID %s (status-code: %d)",
                                    simEntry.iccid,
                                    config.name,
                                    it.statusLine.statusCode),
                            Response.Status.BAD_REQUEST)
                }
            }
        }
    }

    /**
     * Requests the external HLR service to deactivate the SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param simEntry  SIM profile to deactivate
     * @return Updated SIM profile
     */
    override fun suspend(simEntry: SimEntry): SimEntry? {
        if (simEntry.iccid.isEmpty()) {
            throw WebApplicationException(
                    String.format("Illegal parameter in SIM deactivation request to BSSID %s",
                            config.name),
                    Response.Status.BAD_REQUEST)
        }

        val request = RequestBuilder.delete()
                .setUri("${config.endpoint}/deactivate/${simEntry.iccid}")
                .setHeader("x-api-key", config.apiKey)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .build()

        return httpClient.execute(request).use {
            when (it.statusLine.statusCode) {
                200 -> {
                    logger.info("HLR deactivation message to BSSID {} for ICCID {} completed OK",
                            config.name,
                            simEntry.iccid)
                    dao.setHlrState(simEntry.id!!, HssState.NOT_ACTIVATED)
                }
                else -> {
                    logger.warn("HLR deactivation message to BSSID {} for ICCID {} failed with status ({}) {}",
                            config.name,
                            simEntry.iccid,
                            it.statusLine.statusCode,
                            it.statusLine.reasonPhrase)
                    throw WebApplicationException(
                            String.format("Failed to deactivate ICCID %s with BSSID %s (status-code: %d)",
                                    simEntry.iccid,
                                    config.name,
                                    it.statusLine.statusCode),
                            Response.Status.BAD_REQUEST)
                }
            }
        }
    }
}
