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

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("iccid/{iccid}")
    @GET
    fun findByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)
        return simEntry
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("imsi/{imsi}")
    @GET
    fun findByImsi(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("imsi") imsi: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByImsi(imsi))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)
        return simEntry
    }


    @Path("msisdn/{msisdn}")
    @GET
    fun findByMsisdn(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("msisdn") msisdn: String): SimEntry {
        return assertNonNull(dao.getSimProfileByMsisdn(msisdn))
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)

    @Path("msisdn/{msisdn}/allocate-next-free")
    @GET
    fun allocateNextFree(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("msisdn") msisdn: String): SimEntry {
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))
        return assertNonNull(dao.allocateNextFreeSimProfileForMsisdn(hlrAdapter.id, msisdn))
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/iccid/{iccid}/esim/{eid}/all")
    @POST
    fun activateByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String,
            @NotEmpty @PathParam("eid") eid: String): SimEntry {
        activateEsimProfileByIccid(hlr, eid, iccid)
        return activateHlrProfileByIccid(hlr, iccid)
    }

    private fun assertHlrsEqual(hlr1: String, hlr2: String) {
        if (hlr1 != hlr2) {
            throw WebApplicationException(
                    "Attempt to impersonate HLR.  '$hlr1', '$hlr2'",
                    Response.Status.BAD_REQUEST)
        }
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/iccid/{iccid}/esim/{eid}")
    @POST
    fun activateEsimProfileByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String,
            @NotEmpty @PathParam("eid") eid: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)

        val adapter = assertNonNull(dao.getProfileVendorAdapterById(simEntry.profileVendorId))

        try {
            adapter.activate(client, dao, eid, simEntry)
        } catch (e: Exception) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        return assertNonNull(dao.setSmDpPlusState(simEntry.id!!, SmDpPlusState.ACTIVATED))
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/iccid/{iccid}/hlr")
    @POST
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

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/iccid/{iccid}/hlr")
    @DELETE
    fun deactrivateByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)

        hlrAdapter.deactivate(client, dao, simEntry)
        dao.setHlrState(simEntry.id!!, true)
        return dao.getSimProfileById(simEntry.id)
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Throws(IOException::class)
    @Path("/import-batch/profilevendor/{simvendor}")
    @PUT
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
