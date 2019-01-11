package org.ostelco.simcards.adapter

import com.fasterxml.jackson.annotation.JsonProperty
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import javax.ws.rs.client.Client

/**
 * An adapter that connects to a specifc HLR and activate/deactivate
 * individual SIM profiles.
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
     */
    fun activate(client: Client, dao: SimInventoryDAO, simEntry: SimEntry) {
        // XXX TBD
    }

    /**
     * Requests the external HLR service to deactivate the SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param simEntry  SIM profile to deactivate
     */
    fun deactivate(client: Client, dao: SimInventoryDAO, simEntry: SimEntry) {
        // XXX TBD
    }
}
