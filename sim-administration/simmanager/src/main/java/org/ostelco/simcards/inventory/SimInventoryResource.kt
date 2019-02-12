package org.ostelco.simcards.inventory

import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.sim.es2plus.ProfileStatus
import org.ostelco.simcards.admin.HlrConfig
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.admin.SimAdministrationConfiguration
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
    @Path("profileStatusList/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getSimProfileStatus(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): ProfileStatus? {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterById(simEntry.hlrId))
        assertCorrectHlr(hlr, hlr == hlrAdapter.name)

        val simVendorAdapter = assertNonNull(dao.getProfileVendorAdapterById(
                simEntry.profileVendorId))
        val config: ProfileVendorConfig = assertNonNull(config.profileVendors.firstOrNull {
            it.name == simVendorAdapter.name
        })

        return simVendorAdapter.getProfileStatus(client, config, iccid)
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

        return when (simEntry.hlrState) {
            HlrState.NOT_ACTIVATED -> {
                hlrAdapter.activate(client, config, dao, simEntry)
            }
            HlrState.ACTIVATED -> {
                simEntry
            }
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

        return when (simEntry.hlrState) {
            HlrState.NOT_ACTIVATED -> {
                simEntry
            }
            HlrState.ACTIVATED -> {
                hlrAdapter.deactivate(client, config, dao, simEntry)
            }
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
            @NotEmpty @PathParam("msisdn") msisdn: String,
            @DefaultValue("_") @QueryParam("phoneType") phoneType: String): SimEntry {
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))
        val profile = config.getProfileForPhoneType(phoneType)
        return assertNonNull(dao.allocateNextFreeSimProfileForMsisdn(hlrAdapter.id, msisdn, profile))
    }

    @POST
    @Path("esim/all")
    @Produces(MediaType.APPLICATION_JSON)
    fun activateAllEsimProfileByIccid(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @QueryParam("eid") eid: String?,
            @QueryParam("iccid") iccid: String?,
            @DefaultValue("_") @QueryParam("phoneType") phoneType: String): SimEntry? {
        val simEntry = assertNonNull(activateEsimProfileByIccid(hlr, eid, iccid, phoneType))
        return activateHlrProfileByIccid(hlr, simEntry.iccid)
    }

    @POST
    @Path("esim")
    @Produces(MediaType.APPLICATION_JSON)
    fun activateEsimProfileByIccid(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @QueryParam("eid") eid: String?,
            @QueryParam("iccid") iccid: String?,
            @DefaultValue("_") @QueryParam("phoneType") phoneType: String): SimEntry? {
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))
        val profile = config.getProfileForPhoneType(phoneType)
        val simEntry = assertNonNull(if (iccid.isNullOrEmpty())
            dao.findNextFreeSimProfileForHlr(hlrAdapter.id, profile)
        else
            dao.getSimProfileByIccid(iccid))
        assertCorrectHlr(hlr, hlrAdapter.id == simEntry.hlrId)

        val simVendorAdapter = assertNonNull(dao.getProfileVendorAdapterById(
                simEntry.profileVendorId))
        val config: ProfileVendorConfig = assertNonNull(config.profileVendors.filter {
            it.name == simVendorAdapter.name
        }.firstOrNull())

        /* As 'confirm-order' message is issued with 'releaseFlag' set to true, the
           CONFIRMED state should not occur. */
        return when (simEntry.smdpPlusState) {
            SmDpPlusState.AVAILABLE -> {
                simVendorAdapter.activate(client, config, dao, eid, simEntry)
            }
            SmDpPlusState.ALLOCATED -> {
                simVendorAdapter.confirmOrder(client, config, dao, eid, simEntry)
            }
            /* ESIM already 'released'. */
            else -> {
                simEntry
            }
        }
    }

    private fun assertCorrectHlr(hlr: String, match: Boolean) {
        if (!match) {
            throw WebApplicationException("Attempt at impersonating $hlr HLR",
                    Response.Status.BAD_REQUEST)
        }
    }

    @PUT
    @Path("import-batch/profilevendor/{simVendor}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Throws(IOException::class)
    fun importBatch(
            @NotEmpty @PathParam("hlrVendors") hlr: String,
            @NotEmpty @PathParam("simVendor") simVendor: String,
            @NotEmpty @QueryParam("phoneType") phoneType: String,
            csvInputStream: InputStream): SimImportBatch {
        val profileVendorAdapter = assertNonNull(dao.getProfileVendorAdapterByName(simVendor))
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))
        val profile = config.getProfileForPhoneType(phoneType)

        if (!dao.simVendorIsPermittedForHlr(profileVendorAdapter.id, hlrAdapter.id)) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        return dao.importSims(
                importer = "importer", // TODO: This is a very strange name for an importer .-)
                hlrId = hlrAdapter.id,
                profileVendorId = profileVendorAdapter.id,
                profile = profile,
                csvInputStream = csvInputStream)
    }
}
