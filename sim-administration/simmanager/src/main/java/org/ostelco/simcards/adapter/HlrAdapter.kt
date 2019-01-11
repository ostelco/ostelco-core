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
        @JsonProperty("name") val name: String) : Adapter {


    override fun activate(client: Client, dao: SimInventoryDAO, simEntry: SimEntry) {
        // XXX TBD
    }

    override fun deactivate(client: Client, dao: SimInventoryDAO, simEntry: SimEntry) {
        // XXX TBD
    }
}
