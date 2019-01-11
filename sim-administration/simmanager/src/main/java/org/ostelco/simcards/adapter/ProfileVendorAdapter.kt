package org.ostelco.simcards.adapter

import com.fasterxml.jackson.annotation.JsonProperty
import org.ostelco.sim.es2plus.*
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

    /**
     * Requests the external Profile Vendor to activate the
     * SIM profile.
     * @param client  HTTP client
     * @param dao  DB interface
     * @param eid  ESIM id
     * @param simEntry  SIM profile to activate
     */
    fun activate(client: Client, dao: SimInventoryDAO, eid: String, simEntry: SimEntry) {
        val orderStatus = downloadOrder(client, dao, simEntry)
        val confirmStatus = confirmOrder(client, dao, eid, simEntry)
    }

    private fun downloadOrder(client: Client, dao: SimInventoryDAO, simEntry: SimEntry) : Es2DownloadOrderResponse {
        val header = ES2RequestHeader(
                functionRequesterIdentifier = "",
                functionCallIdentifier = ""
        )
        val payload = Es2PlusDownloadOrder(
                header = header,
                iccid = simEntry.iccid
        )
        val response = client.target("http://localhost:9080/gsma/rsp2/es2plus/downloadOrder")
                .request()
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON))

        if (response.status != 200) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }

        val status = response.readEntity(Es2DownloadOrderResponse::class.java)

        return if (status.header.functionExecutionStatus.status != FunctionExecutionStatusType.ExecutedSuccess)
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        else {
            dao.setSmDpPlusState(simEntry.id!!, SmDpPlusState.ORDER_DOWNLOADED)
            status
        }
    }

    private fun confirmOrder(client: Client, dao: SimInventoryDAO, eid: String, simEntry: SimEntry) : Es2ConfirmOrderResponse {
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
        val response = client.target("http://localhost:9080/gsma/rsp2/es2plus/confirmOrder")
                .request()
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON))

        if (response.status != 200) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }

        val status = response.readEntity(Es2ConfirmOrderResponse::class.java)

        return if (status.header.functionExecutionStatus.status != FunctionExecutionStatusType.ExecutedSuccess)
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        else {
            dao.setSmDpPlusState(simEntry.id!!, SmDpPlusState.ACTIVATED)
            status
        }
    }
}