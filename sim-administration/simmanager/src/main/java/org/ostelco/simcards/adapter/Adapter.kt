package org.ostelco.simcards.adapter

import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import javax.ws.rs.client.Client

interface Adapter {

    /**
     * Requests the external service (typically a HLR or a SIM profile
     * vendor) to activate the SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param simEntry  SIM profile to activate
     */
    fun activate(client: Client, dao: SimInventoryDAO, simEntry: SimEntry)

    /**
     * Requests the external service to deactivate the SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param simEntry  SIM profile to deactivate
     */
    fun deactivate(client: Client, dao: SimInventoryDAO, simEntry: SimEntry)
}