package org.ostelco.simcards.adapter

import org.ostelco.simcards.admin.HlrConfig
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
interface HlrAdapter {
    val id: Long
    val name: String

    /**
     * Requests the external HLR service to activate the SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    fun activate(client: Client, config: HlrConfig, dao: SimInventoryDAO, simEntry: SimEntry) : SimEntry?

    /**
     * Requests the external HLR service to deactivate the SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param simEntry  SIM profile to deactivate
     * @return Updated SIM profile
     */
    fun deactivate(client: Client, config: HlrConfig, dao: SimInventoryDAO, simEntry: SimEntry) : SimEntry?
}
