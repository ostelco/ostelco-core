package org.ostelco.simcards.inventory

import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.simcards.admin.HlrConfig
import org.ostelco.simcards.admin.SimAdministrationConfiguration
import org.ostelco.simcards.admin.ProfileVendorConfig
import java.io.IOException
import java.io.InputStream
import javax.ws.rs.*
import javax.ws.rs.client.Client
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


///
///  The web resource using the protocol domain model.
///

@Path("/ostelco/sim-inventory/{hlrVendors}")
class SimInventoryResource(private val client: Client,
                           private val config: SimAdministrationConfiguration,
                           private val dao: SimInventoryDAO) {

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
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertCorrectHlr(hlr, hlr == hlrAdapter.name)
        return simEntry
    }

    @POST
    @Path("iccid/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun activateHlrProfileByIccid(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry? {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertCorrectHlr(hlr, hlr == hlrAdapter.name)

        val config: HlrConfig = assertNonNull(config.hlrVendors.filter {
            it.name == hlrAdapter.name
        }.firstOrNull())

        return try {
            hlrAdapter.activate(client, config, dao, simEntry)
        } catch (e: Exception) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
    }

    @DELETE
    @Path("iccid/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun deactivateHlrProfileByIccid(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry? {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertCorrectHlr(hlr, hlr == hlrAdapter.name)

        val config: HlrConfig = assertNonNull(config.hlrVendors.filter {
            it.name == hlrAdapter.name
        }.firstOrNull())

        return try {
            hlrAdapter.deactivate(client, config, dao, simEntry)
        } catch (e: Exception) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
    }

    @GET
    @Path("imsi/{imsi}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByImsi(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @NotEmpty @PathParam("imsi") imsi: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByImsi(imsi))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertCorrectHlr(hlr, hlr == hlrAdapter.name)
        return simEntry
    }

    @GET
    @Path("msisdn/{msisdn}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findByMsisdn(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @NotEmpty @PathParam("msisdn") msisdn: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByMsisdn(msisdn))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertCorrectHlr(hlr, hlr == hlrAdapter.name)
        return simEntry
    }

    @GET
    @Path("msisdn/{msisdn}/next-free")
    @Produces(MediaType.APPLICATION_JSON)
    fun allocateSimProfileForMsisdn(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @NotEmpty @PathParam("msisdn") msisdn: String): SimEntry {
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))
        return assertNonNull(dao.allocateNextFreeSimProfileForMsisdn(hlrAdapter.id, msisdn))
    }

    @POST
    @Path("esim/{eid}/all")
    @Produces(MediaType.APPLICATION_JSON)
    fun activateAllEsimProfileByIccid(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @NotEmpty @PathParam("eid") eid: String,
            @QueryParam("iccid") iccid: String?): SimEntry? {
        val simEntry = assertNonNull(activateEsimProfileByIccid(hlr, eid, iccid))
        return activateHlrProfileByIccid(hlr, simEntry.iccid)
    }

    @POST
    @Path("esim/{eid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun activateEsimProfileByIccid(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @NotEmpty @PathParam("eid") eid: String,
            @QueryParam("iccid") iccid: String?): SimEntry? {
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))
        val simEntry = assertNonNull(if (iccid.isNullOrEmpty())
            dao.findNextFreeSimProfileForHlr(hlrAdapter.id)
        else
            dao.getSimProfileByIccid(iccid))
        assertCorrectHlr(hlr, hlrAdapter.id == simEntry.hlrId)

        val simVendorAdapter = assertNonNull(dao.getProfileVendorAdapterById(
                simEntry.profileVendorId))
        val config: ProfileVendorConfig = assertNonNull(config.profileVendors.filter {
            it.name == simVendorAdapter.name
        }.firstOrNull())

        return try {
            simVendorAdapter.activate(client, config, dao, eid, simEntry)
        } catch (e: Exception) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
    }

    private fun assertCorrectHlr(hlr: String, match: Boolean) {
        if (!match) {
            throw WebApplicationException("Attempt at impersonating ${hlr} HLR",
                    Response.Status.BAD_REQUEST)
        }
    }

    @PUT
    @Path("import-batch/profilevendor/{simvendor}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Throws(IOException::class)
    fun importBatch(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @NotEmpty @PathParam("simvendor") simVendor: String,
            csvInputStream: InputStream): SimImportBatch {
        val profileVendorAdapter = assertNonNull(dao.getProfileVendorAdapterByName(simVendor))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))

        if (!dao.simVendorIsPermittedForHlr(profileVendorAdapter.id, hlrAdapter.id)) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        return dao.importSims(
                importer = "importer", // TODO: This is a very strange name for an importer .-)
                hlrId = hlrAdapter.id,
                profileVendorId = profileVendorAdapter.id,
                csvInputStream = csvInputStream)
    }
}
