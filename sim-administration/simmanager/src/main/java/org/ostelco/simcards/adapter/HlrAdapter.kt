package org.ostelco.simcards.adapter

import arrow.core.Either
import arrow.core.left
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.getLogger
import org.ostelco.prime.simmanager.AdapterError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.admin.HlrConfig
import org.ostelco.simcards.inventory.*
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * An adapter that connects to a HLR and activates/deactivates individual
 * SIM profiles.
 *
 * When a VLR asks the HLR for the an authentication triplet, then the
 * HLR will know that it should give an answer.
 */
data class HlrAdapter(
        @JsonProperty("id") val id: Long,
        @JsonProperty("name") val name: String) {

    private val logger by getLogger()

    /* For payload serializing. */
    private val mapper = jacksonObjectMapper()

    /**
     * Requests the external HLR service to activate the SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    fun activate(httpClient: CloseableHttpClient,
                 config: HlrConfig,
                 dao: SimInventoryDAO,
                 simEntry: SimEntry): Either<SimManagerError, SimEntry> {
        if (simEntry.iccid.isEmpty()) {
            return AdapterError("Illegal parameter in SIM activation request to BSSID ${config.name}")
                    .left()
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
                    dao.setHlrState(simEntry.id!!, HlrState.ACTIVATED)
                }
                else -> {
                    logger.warn("HLR activation message to BSSID {} for ICCID {} failed with status ({}) {}",
                            config.name,
                            simEntry.iccid,
                            it.statusLine.statusCode,
                            it.statusLine.reasonPhrase)
                    AdapterError("Failed to activate ICCID ${simEntry.iccid} with BSSID ${config.name} (status-code: ${it.statusLine.statusCode})")
                            .left()
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
    fun deactivate(httpClient: CloseableHttpClient,
                   config: HlrConfig,
                   dao: SimInventoryDAO,
                   simEntry: SimEntry): Either<SimManagerError, SimEntry> {
        if (simEntry.iccid.isEmpty()) {
            return AdapterError("Illegal parameter in SIM deactivation request to BSSID ${config.name}")
                    .left()
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
                    dao.setHlrState(simEntry.id!!, HlrState.NOT_ACTIVATED)
                }
                else -> {
                    logger.warn("HLR deactivation message to BSSID {} for ICCID {} failed with status ({}) {}",
                            config.name,
                            simEntry.iccid,
                            it.statusLine.statusCode,
                            it.statusLine.reasonPhrase)
                    AdapterError("Failed to deactivate ICCID ${simEntry.iccid} with BSSID ${config.name} (status-code: ${it.statusLine.statusCode}")
                            .left()
                }
            }
        }
    }
}