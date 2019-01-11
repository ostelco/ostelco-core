package org.ostelco.simcards.adapter

import com.fasterxml.jackson.annotation.JsonProperty
import org.ostelco.sim.es2plus.*
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
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
        @JsonProperty("name") val name: String) : Adapter {

    override fun activate(client: Client, dao: SimInventoryDAO, simEntry: SimEntry) {
        val orderStatus = downloadOrder(client, simEntry.iccid)
        val confirmStatus = confirmOrder(client, "01010101010101010101010101010101", simEntry.iccid)
    }

    override fun deactivate(client: Client, dao: SimInventoryDAO, simEntry: SimEntry) {
        // XXX TBD
    }

    private fun downloadOrder(client: Client, iccid: String) : Es2DownloadOrderResponse {
        val header = ES2RequestHeader(
                functionRequesterIdentifier = "",
                functionCallIdentifier = ""
        )
        val payload = Es2PlusDownloadOrder(
                header = header,
                iccid = iccid
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
            status
        }
    }

    private fun confirmOrder(client: Client, eid: String, iccid: String) : Es2ConfirmOrderResponse {
        val header = ES2RequestHeader(
                functionRequesterIdentifier = "",
                functionCallIdentifier = ""
        )
        val payload = Es2ConfirmOrder(
                header = header,
                eid = eid,
                iccid = iccid,
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
        else
            status
    }
}