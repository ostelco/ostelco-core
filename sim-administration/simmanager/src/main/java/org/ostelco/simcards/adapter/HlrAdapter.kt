package org.ostelco.simcards.adapter

import com.fasterxml.jackson.annotation.JsonProperty
import org.ostelco.simcards.admin.HlrConfig
import org.ostelco.simcards.admin.getLogger
import org.ostelco.simcards.inventory.HlrState
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
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

    /**
     * Requests the external HLR service to activate the SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    fun activate(client: Client, config: HlrConfig, dao: SimInventoryDAO, simEntry: SimEntry) : SimEntry? {
        if (simEntry.iccid.isEmpty()) {
            throw WebApplicationException(
                    String.format("Illegal parameter in SIM activation request to BSSID %s",
                            config.name),
                    Response.Status.BAD_REQUEST)
        }
        val payload = mapOf(
                "bssid" to config.name,
                "profileStatusList" to simEntry.iccid,
                "msisdn" to simEntry.msisdn,
                "userid" to config.userId
        )
        val response = client.target("${config.endpoint}/activate")
                .request(MediaType.APPLICATION_JSON)
                .header("x-api-key", config.apiKey)
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON))
        if (response.status != 201) {
            throw WebApplicationException(
                    String.format("Failed to deactivate ICCID %s with BSSID %s",
                            simEntry.iccid,
                            config.name),
                    Response.Status.BAD_REQUEST)
        }

        logger.info("HLR activation message to BSSID {} for ICCID {} completed OK",
                config.name,
                simEntry.iccid)
        return dao.setHlrState(simEntry.id!!, HlrState.ACTIVATED)
    }

    /**
     * Requests the external HLR service to deactivate the SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param simEntry  SIM profile to deactivate
     * @return Updated SIM profile
     */
    fun deactivate(client: Client, config: HlrConfig, dao: SimInventoryDAO, simEntry: SimEntry) : SimEntry? {
        if (simEntry.iccid.isEmpty()) {
            throw WebApplicationException(
                    String.format("Illegal parameter in SIM deactivation request to BSSID %s",
                            config.name),
                    Response.Status.BAD_REQUEST)
        }
        val response = client.target("${config.endpoint}/deactivate/${simEntry.iccid}")
                .request(MediaType.APPLICATION_JSON)
                .header("x-api-key", config.apiKey)
                .delete()
        if (response.status != 200) {
            throw WebApplicationException(
                    String.format("Failed to deactivate ICCID %s with BSSID %s",
                            simEntry.iccid,
                            config.name),
                    Response.Status.BAD_REQUEST)
        }

        logger.info("HLR deactivation message to BSSID {} for ICCID {} completed OK",
                config.name,
                simEntry.iccid)
        return dao.setHlrState(simEntry.id!!, HlrState.NOT_ACTIVATED)
    }
}