package org.ostelco.simcards.inventory

import org.apache.http.impl.client.CloseableHttpClient
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.sim.es2plus.ProfileStatus
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.admin.SimAdministrationConfiguration
import java.io.IOException
import java.io.InputStream
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


///
///  The web resource using the protocol domain model.
///

@Path("/ostelco/sim-inventory/{hssVendors}")
class SimInventoryResource(private val httpClient: CloseableHttpClient,
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
            @NotEmpty @PathParam("hssVendors") hss: String,
            @NotEmpty @PathParam("iccid") iccid: String): ProfileStatus? {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hssAdapter = assertNonNull(dao.getHssEntryById(simEntry.hssId))
        assertCorrectHss(hss, hss == hssAdapter.name)

        val simVendorAdapter = assertNonNull(dao.getProfileVendorAdapterById(
                simEntry.profileVendorId))
        val config: ProfileVendorConfig = assertNonNull(config.profileVendors.firstOrNull {
            it.name == simVendorAdapter.name
        })

        return simVendorAdapter.getProfileStatus(httpClient, config, iccid)
    }

    @GET
    @Path("iccid/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByIccid(
            @NotEmpty @PathParam("hssVendors") hss: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByIccid(iccid))
        val hssAdapter = assertNonNull(dao.getHssEntryById(simEntry.hssId))
        assertCorrectHss(hss, hss == hssAdapter.name)
        return simEntry
    }


    @GET
    @Path("imsi/{imsi}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByImsi(
            @NotEmpty @PathParam("hssVendors") hss: String,
            @NotEmpty @PathParam("imsi") imsi: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByImsi(imsi))
        val hssAdapter = assertNonNull(dao.getHssEntryById(simEntry.hssId))
        assertCorrectHss(hss, hss == hssAdapter.name)
        return simEntry
    }

    @GET
    @Path("msisdn/{msisdn}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findByMsisdn(
            @NotEmpty @PathParam("hssVendors") hss: String,
            @NotEmpty @PathParam("msisdn") msisdn: String): SimEntry {
        val simEntry = assertNonNull(dao.getSimProfileByMsisdn(msisdn))
        val hssAdapter = assertNonNull(dao.getHssEntryById(simEntry.hssId))
        assertCorrectHss(hss, hss == hssAdapter.name)
        return simEntry
    }


    @GET
    @Path("esim")
    @Produces(MediaType.APPLICATION_JSON)
    fun allocateNextEsimProfile(
            @NotEmpty @PathParam("hssVendors") hss: String,
            @DefaultValue("_") @QueryParam("phoneType") phoneType: String): SimEntry? {
        val hssEntry = assertNonNull(dao.getHssEntryByName(hss))
        val profile = config.getProfileForPhoneType(phoneType)
        val simEntry = assertNonNull(dao.findNextReadyToUseSimProfileForHlr(hssEntry.id,
                profile))
        assertCorrectHss(hss, hssEntry.id == simEntry.hssId)

        val simVendorAdapter = assertNonNull(dao.getProfileVendorAdapterById(
                simEntry.profileVendorId))
        val profileVendorConfig: ProfileVendorConfig = assertNonNull(config.profileVendors.filter {
            it.name == simVendorAdapter.name
        }.firstOrNull())

        /* Add 'code' field content. */
        return assertNonNull(dao.setProvisionState(simEntry.id!!,
                ProvisionState.PROVISIONED)!!.let {
            it.copy(code = "LPA:${profileVendorConfig.es9plusEndpoint}:${it.matchingId}")
        })
    }

    private fun assertCorrectHss(hss: String, match: Boolean) {
        if (!match) {
            throw WebApplicationException("Attempt at impersonating $hss HLR",
                    Response.Status.BAD_REQUEST)
        }
    }

    @PUT
    @Path("import-batch/profilevendor/{simVendor}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Throws(IOException::class)
    fun importBatch(
            @NotEmpty @PathParam("hssVendors") hss: String,
            @NotEmpty @PathParam("simVendor") simVendor: String,
            csvInputStream: InputStream): SimImportBatch? {
        val profileVendorAdapter = assertNonNull(dao.getProfileVendorAdapterByName(simVendor))
        val hssAdapter = assertNonNull(dao.getHssEntryByName(hss))

        if (!dao.simVendorIsPermittedForHlr(profileVendorAdapter.id, hssAdapter.id)) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        return assertNonNull(dao.importSims(
                importer = "importer", // TODO: This is a very strange name for an importer .-)
                hssId = hssAdapter.id,
                profileVendorId = profileVendorAdapter.id,
                csvInputStream = csvInputStream))
    }
}
