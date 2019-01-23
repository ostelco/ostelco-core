package org.ostelco.simcards.adapter

import com.fasterxml.jackson.annotation.JsonProperty
import org.ostelco.simcards.admin.HlrConfig
import org.ostelco.simcards.inventory.HlrState
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
data class Wg2HlrAdapter(
        @JsonProperty("id") override  val id: Long,
        @JsonProperty("name") override val name: String) : HlrAdapter {

    override fun activate(client: Client, config: HlrConfig, dao: SimInventoryDAO, simEntry: SimEntry) : SimEntry? {
        return dao.setHlrState(simEntry.id!!, HlrState.ACTIVATED)
    }

    override fun deactivate(client: Client, config: HlrConfig, dao: SimInventoryDAO, simEntry: SimEntry) : SimEntry? {
        return dao.setHlrState(simEntry.id!!, HlrState.NOT_ACTIVATED)
    }
}