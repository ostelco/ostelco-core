package org.ostelco.simcards.inventory

import org.hibernate.validator.constraints.NotEmpty
import java.io.IOException
import java.io.InputStream
import javax.ws.rs.*
import javax.ws.rs.client.Client
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


///
///  The web resource using the protocol domain model.
///

@Path("/ostelco/sim-inventory/{hlr}")
class SimInventoryResource(private val client: Client, private val dao: SimInventoryDAO) {

    companion object {
        private fun <T> assertNonNull(v: T?): T {
            if (v == null) {
                throw WebApplicationException(Response.Status.NOT_FOUND)
            } else {
                return v
            }
        }
    }

    @GET
    @Path("iccid/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)
        return simEntry
    }

    @POST
    @Path("iccid/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun activateHlrProfileByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)

        try {
            hlrAdapter.activate(client, dao, simEntry)
        } catch (e: Exception) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        return assertNonNull(dao.setHlrState(simEntry.id!!, true))
    }

    @DELETE
    @Path("iccid/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun deactivateHlrProfileByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)

        try {
            hlrAdapter.deactivate(client, dao, simEntry)
        } catch (e: Exception) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        return assertNonNull(dao.setHlrState(simEntry.id!!, false))
    }

    @GET
    @Path("imsi/{imsi}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByImsi(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("imsi") imsi: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByImsi(imsi))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)
        return simEntry
    }

    @GET
    @Path("msisdn/{msisdn}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findByMsisdn(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("msisdn") msisdn: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByMsisdn(msisdn))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)
        return simEntry
    }

    @GET
    @Path("msisdn/{msisdn}/next-free")
    @Produces(MediaType.APPLICATION_JSON)
    fun allocateSimProfileForMsisdn(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("msisdn") msisdn: String): SimEntry {
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))
        return assertNonNull(dao.allocateNextFreeSimProfileForMsisdn(hlrAdapter.id, msisdn))
    }

    @POST
    @Path("esim/{eid}/all")
    @Produces(MediaType.APPLICATION_JSON)
    fun activateAllEsimProfileByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("eid") eid: String,
            @QueryParam("iccid") iccid: String?): SimEntry {
        val simEntry = assertNonNull(activateEsimProfileByIccid(hlr, eid, iccid))
        return activateHlrProfileByIccid(hlr, simEntry.iccid)
    }

    @POST
    @Path("esim/{eid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun activateEsimProfileByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("eid") eid: String,
            @QueryParam("iccid") iccid: String?): SimEntry {
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))
        val simEntry = assertNonNull(if (iccid.isNullOrEmpty())
            dao.findNextFreeSimProfileForHlr(hlrAdapter.id)
        else
            dao.getSimProfileByIccid(iccid))
        assertHlrsEqual(hlrAdapter.id, simEntry.hlrId)

        val simVendorAdapter = assertNonNull(dao.getProfileVendorAdapterById(simEntry.profileVendorId))

        try {
            simVendorAdapter.activate(client, dao, eid, simEntry)
        } catch (e: Exception) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        return assertNonNull(dao.setSmDpPlusState(simEntry.id!!, SmDpPlusState.ACTIVATED))
    }

    private fun assertHlrsEqual(hlr1: String, hlr2: String) {
        if (hlr1 != hlr2) {
            throw WebApplicationException(
                    "Attempt to impersonate HLR.  '$hlr1', '$hlr2'",
                    Response.Status.BAD_REQUEST)
        }
    }

    private fun assertHlrsEqual(hlrId1: Long, hlrId2: Long) {
        if (hlrId1 != hlrId2) {
            throw WebApplicationException(
                    "Attempt to impersonate HLR.  '$hlrId1', '$hlrId2'",
                    Response.Status.BAD_REQUEST)
        }
    }

    @PUT
    @Path("import-batch/profilevendor/{simvendor}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Throws(IOException::class)
    fun importBatch(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("simvendor") simVendor: String,
            csvInputStream: InputStream): SimImportBatch {
        val simVendorAdapter = assertNonNull(dao.getProfileVendorAdapterByName(simVendor))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))

        if (!dao.simVendorIsPermittedForHlr(simVendorAdapter.id, hlrAdapter.id)) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        return dao.importSims(
                importer = "importer", // TODO: This is a very strange name for an importer .-)
                hlrId = hlrAdapter.id,
                profileVendorId = simVendorAdapter.id,
                csvInputStream = csvInputStream)
    }
}
