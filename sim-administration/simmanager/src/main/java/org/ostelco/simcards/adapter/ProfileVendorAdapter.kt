package org.ostelco.simcards.adapter

import com.fasterxml.jackson.annotation.JsonProperty
import org.ostelco.sim.es2plus.*
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.admin.getLogger
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SmDpPlusState
import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * An adapter that can connect to SIM profile vendors and activate
 * the requested SIM profile.
 *
 * Will connect to the SM-DP+  and then activate the profile, so that when
 * user equpiment tries to download a profile, it will get a profile to
 * download.
 */
data class ProfileVendorAdapter (
        @JsonProperty("id") val id: Long,
        @JsonProperty("name") val name: String) {

    private val logger by getLogger()

    /**
     * Requests the an external Profile Vendor to activate the
     * SIM profile.
     * @param client  HTTP client
     * @param config SIM vendor specific configuration
     * @param dao  DB interface
     * @param eid  ESIM id
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    fun activate(client: Client,
                 config: ProfileVendorConfig,
                 dao: SimInventoryDAO,
                 eid: String?,
                 simEntry: SimEntry) : SimEntry? {
        return if (downloadOrder(client, config, dao, simEntry) != null)
            confirmOrder(client, config, dao, eid, simEntry)
         else
            null
    }

    /* XXX Update SM-DP+ 'header' to correct content. */

    /**
     * Initiate activation of a SIM profile with an external Profile Vendor
     * by sending a SM-DP+ 'download-order' message.
     * @param client  HTTP client
     * @param config SIM vendor specific configuration
     * @param dao  DB interface
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    fun downloadOrder(client: Client,
                      config: ProfileVendorConfig,
                      dao: SimInventoryDAO,
                      simEntry: SimEntry) : SimEntry? {
        val header = ES2RequestHeader(
                functionRequesterIdentifier = "",
                functionCallIdentifier = ""
        )
        val payload = Es2PlusDownloadOrder(
                header = header,
                iccid = simEntry.iccid
        )
        val response = client.target("${config.endpoint}/downloadOrder")
                .request()
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON))

        if (response.status != 200) {
            throw WebApplicationException(String.format("Order download to SM-DP+ service %s for ICCID %s failed with status code %d",
                    config.name, simEntry.iccid, response.status),
                    Response.Status.BAD_REQUEST)
        }

        val status = response.readEntity(Es2DownloadOrderResponse::class.java)

        return if (status.header.functionExecutionStatus.status != FunctionExecutionStatusType.ExecutedSuccess)
            throw WebApplicationException(String.format("Order download to SM-DP+ service %s for ICCID %s failed with execution status %s",
                    config.name, simEntry.iccid, status.header.functionExecutionStatus),
                    Response.Status.BAD_REQUEST)
        else
            dao.setSmDpPlusState(simEntry.id!!, SmDpPlusState.ORDER_DOWNLOADED)
    }

    /**
     * Complete the activation of a SIM profile with an external Profile Vendor
     * by sending a SM-DP+ 'confirmation' message.
     * @param client  HTTP client
     * @param config SIM vendor specific configuration
     * @param dao  DB interface
     * @param eid  ESIM id
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    fun confirmOrder(client: Client,
                     config: ProfileVendorConfig,
                     dao: SimInventoryDAO,
                     eid: String?,
                     simEntry: SimEntry) : SimEntry? {
        val header = ES2RequestHeader(
                functionRequesterIdentifier = "",
                functionCallIdentifier = ""
        )
        val payload = Es2ConfirmOrder(
                header = header,
                eid = eid,
                iccid = simEntry.iccid,
                releaseFlag = true
        )
        val response = client.target("${config.endpoint}/confirmOrder")
                .request()
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON))

        if (response.status != 200) {
            throw WebApplicationException(String.format("Order confirm messageto SM-DP+ service %s for ICCID %s failed with status code %d",
                    config.name, simEntry.iccid, response.status),
                    Response.Status.BAD_REQUEST)
        }

        val status = response.readEntity(Es2ConfirmOrderResponse::class.java)

        return if (status.header.functionExecutionStatus.status != FunctionExecutionStatusType.ExecutedSuccess)
            throw WebApplicationException(String.format("Order confirm message to SM-DP+ service %s for ICCID %s failed with execution status %s",
                    config.name, simEntry.iccid, status.header.functionExecutionStatus),
                    Response.Status.BAD_REQUEST)
        else {
            // XXX Is just logging good enough?
            if (status.eid.isEmpty()) {
                logger.warn("No EID returned from SM-DP+ service {} for ICCID {}",
                        config.name, simEntry.iccid)
            } else {
                dao.setEidOfSimProfile(simEntry.id!!, status.eid)     /* Update profile with 'eid' value. */
            }
            if (!eid.isNullOrEmpty() && eid != status.eid) {
                logger.warn("EID returned from SM-DP+ service {} does not match provided EID ({} <> {})",
                        config.name, eid, status.eid)
            }
            dao.setSmDpPlusState(simEntry.id!!, SmDpPlusState.ACTIVATED)
        }
    }
}