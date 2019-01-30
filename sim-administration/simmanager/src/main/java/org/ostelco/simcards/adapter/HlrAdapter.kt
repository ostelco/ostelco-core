package org.ostelco.simcards.adapter

import com.fasterxml.jackson.annotation.JsonProperty
import org.ostelco.simcards.admin.HlrConfig
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

    /**
     * Requests the external HLR service to activate the SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    fun activate(client: Client, config: HlrConfig, dao: SimInventoryDAO, simEntry: SimEntry) : SimEntry? {
        val payload = mapOf(
                "bssid" to config.name,
                "iccid" to simEntry.iccid,
                "msidn" to simEntry.msisdn,
                "userid" to config.userId
        )
        val response = client.target("${config.url}/acivate")
                .request(MediaType.APPLICATION_JSON)
                .header("x-api-key", config.apiKey)
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON))

        if (response.status != 201) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }

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
        return dao.setHlrState(simEntry.id!!, HlrState.NOT_ACTIVATED)
    }
}