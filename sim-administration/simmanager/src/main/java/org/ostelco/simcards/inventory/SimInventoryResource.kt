package org.ostelco.simcards.inventory

import org.hibernate.validator.constraints.NotEmpty
import java.io.IOException
import java.io.InputStream
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


///
///  The web resource using the protocol domain model.
///

@Path("/ostelco/sim-inventory/{hlr}")
class SimInventoryResource(private val dao: SimInventoryDAO) {

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
        return assertNonNull(dao.allocateNextFreeSimForMsisdn(hlr, msisdn))
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/iccid/{iccid}/activate/all")
    @GET
    fun activateByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {
        val simEntry = activateHlrProfileByIccid(hlr, iccid)
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)

        return if (simEntry.smdpplus != null) {
            activateEsimProfileByIccid(hlr, iccid)
        } else {
            simEntry
        }
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
    @Path("/iccid/{iccid}/activate/hlr")
    @GET
    fun activateHlrProfileByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {

        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)

        try {
            hlrAdapter.activate(simEntry)
        } catch (e: Exception) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        return assertNonNull(dao.setActivatedInHlr(simEntry.id!!))
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/iccid/{iccid}/activate/esim")
    @GET
    fun activateEsimProfileByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)

        if (simEntry.smdpplus == null) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        val smdpPlusAdapter = assertNonNull(dao.getSmdpPlusAdapterByName(simEntry.smdpplus))

        try {
            smdpPlusAdapter.activateEntry(simEntry)
        } catch (e: Exception) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        return assertNonNull(dao.setActivatedInSmdpPlus(simEntry.id!!))
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/iccid/{iccid}/deactivate/hlr")
    @GET
    fun deactrivateByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertHlrsEqual(hlr, hlrAdapter.name)

        hlrAdapter.deactivate(simEntry)
        dao.setActivatedInHlr(simEntry.id!!)
        return dao.getSimProfileById(simEntry.id)
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Throws(IOException::class)
    @Path("/import-batch/profilevendor/{profilevendor}")
    @PUT
    fun importBatch(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("profilevendor") profilevendor: String,
            csvInputStream: InputStream): SimImportBatch {

        val profileVendor = assertNonNull(dao.getProfilevendorByName(profilevendor))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))

        if (!dao.simVendorIsPermittedForHlr(profileVendor.id, hlrAdapter.id)) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }

        return dao.importSims(
                importer = "importer", // TODO: This is a very strange name for an importer .-)
                hlrId = hlrAdapter.id,
                profileVendorId = profileVendor.id,
                csvInputStream = csvInputStream)
    }
}
