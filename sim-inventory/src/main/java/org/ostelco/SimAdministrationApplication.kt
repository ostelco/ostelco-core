package org.ostelco

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.hibernate.validator.constraints.NotEmpty
import java.io.IOException
import java.io.InputStream
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


/**
 * The SIM Inventory  (a slight misnomer,  look for a better name)
 * is an application that inputs inhales SIM batches
 * from SIM profile factories (physical or esim). It then facilitates
 * activation of SIM profiles to MSISDNs.   A typical interaction is
 * "find me a sim profile for this MSISDN for this HLR" , and then
 * "activate that profile".   The activation will typically involve
 * at least talking to a HLR to permit user equipment to use the
 * SIM profile to authenticate, and possibly also an SM-DP+ to
 * activate a SIM profile (via its ICCID and possible an EID).
 * The inventory can then serve as an intermidiary between the
 * rest of the BSS and the OSS in the form of HSS and SM-DP+.
 */
class SimAdministrationApplication : Application<SimAdministrationAppConfiguration>() {

    override fun getName(): String {
        return "SIM inventory application"
    }

    override fun initialize(bootstrap: Bootstrap<SimAdministrationAppConfiguration>) {
        // TODO: application initialization
    }

    lateinit var simInventoryDAO: SimInventoryDAO

    override fun run(configuration: SimAdministrationAppConfiguration,
                     environment: Environment) {


        val factory = DBIFactory()
        val jdbi = factory.build(
                environment,
                configuration.database, "sqlite")
        this.simInventoryDAO = jdbi.onDemand(SimInventoryDAO::class.java)


        // XXX Add these parameters to configuration file.
        val oas = OpenAPI()
        val info = Info()
                .title(getName())
                .description("SIM management.")
                .termsOfService("http://example.com/terms")
                .contact(Contact().email("rmz@redotter.com"))

        oas.info(info)
        val oasConfig = SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .resourcePackages(Stream.of("org.ostelco")
                        .collect(Collectors.toSet<String>()))
        environment.jersey().register(OpenApiResource()
                .openApiConfiguration(oasConfig))

        environment.jersey().register(EsimInventoryResource(simInventoryDAO))
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SimAdministrationApplication().run(*args)
        }
    }
}

/*


// Starting point for intercepting exceptions, so that they can
// be wrapped in a return value of sorts.

class Es2Exception extends Exception {
}


class AppExceptionMapper : ExceptionMapper<Es2Exception> {
    fun toResponse(ex: Es2Exception): Response {
        return Response.status(ex.getStatus())
                .entity(ErrorMessage(ex))
                .type(MediaType.APPLICATION_JSON).build()
    }
}
*/

///
///  The web resource using the protocol domain model.
///

@Path("/ostelco/sim-inventory/{hlr}/")
class EsimInventoryResource(val dao: SimInventoryDAO) {

    private fun <T> assertNonNull(v: T?): T {
        if (v == null) {
            throw WebApplicationException(Response.Status.NOT_FOUND)
        } else {
            return v
        }
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("iccid/{iccid}")
    @GET
    fun findByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String): SimEntry {

        val sim = assertNonNull(dao.getSimProfileByIccid(iccid))
        assertHlrsEqual(hlr, sim.hlrId)
        return sim
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("imsi/{imsi}")
    @GET
    fun findByImsi(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("imsi") imsi: String): SimEntry {
        val sim = assertNonNull(dao.getSimProfileByImsi(imsi))
        assertHlrsEqual(hlr, sim.hlrId)
        return sim
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
        val sim = activateHlrProfileByIccid(hlr, iccid)
        assertHlrsEqual(hlr, sim.hlrId)
        if (sim.smdpplus != null) {
            return activateEsimProfileByIccid(hlr, iccid)
        } else {
            return sim
        }
    }

    fun assertHlrsEqual(hlr1: String, hlr2: String) {
        if (!hlr1.equals(hlr2)) {
            // XXX log.error("Attempt to impersonate HLR.  '$hlr1', '$hlr2'")
            throw WebApplicationException(Response.Status.BAD_REQUEST)
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
        assertHlrsEqual(hlr, simEntry.hlrId)
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))

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
        assertHlrsEqual(hlr, simEntry.hlrId)

        if (simEntry.smdpplus == null) {
            throw WebApplicationException(Response.Status.BAD_REQUEST)
        }
        val smdpPlusAdpter = assertNonNull(dao.getSmdpPlusAdapterByName(simEntry.smdpplus))

        try {
            smdpPlusAdpter.activateEntry(simEntry)
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

        assertHlrsEqual(hlr, simEntry.hlrId)
        val hlrAdapter = assertNonNull(dao.getHlrAdapterByName(hlr))

        hlrAdapter.deactivate(simEntry)
        dao.setActivatedInHlr(simEntry.id!!)
        return dao.getSimProfileById(simEntry.id!!)
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Throws(IOException::class)
    @Path("/import-batch/sim-profile-vendor/{profilevendor}")
    @PUT
    fun importBatch(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("profilevendor") profilevendor: String,
            csvInputStream: InputStream): SimImportBatch {

        // XXX Check referential integrity of hlr and profilevendor
        //      Also add a parameter for importer, and c heck if that importer
        //      has the necessary permissions to perform imports.

        return dao.importSims(
                importer = "importer",
                hlr = hlr,
                profileVendor = profilevendor,
                csvInputStream = csvInputStream)
    }
}


/**
 * An adapter that can connect to HLR entries and activate/deactivate
 * individual SIM profiles.
 */
data class HlrAdapter(
        @JsonProperty("id") val id: Long,
        @JsonProperty("name") val name: String) {

    /**
     * Will connect to the HLR and then activate the profile, so that when
     * a VLR asks the HLR for the an authentication triplet, then the HLR
     * will know that it should give an answer.
     */
    fun activate(simEntry: SimEntry) {
        // XXX TBD
    }

    fun deactivate(simEntry: SimEntry) {
        // XXX TBD
    }
}


/**
 * An adapter that can connect to HLR entries and activate/deactivate
 * individual SIM profiles.
 */
data class SmdpPlusAdapter(
        @JsonProperty("id") val id: Long,
        @JsonProperty("name") val name: String) {

    /**
     * Will connect to the SM-DP+  and then activate the profile, so that when
     * user equpiment tries to download a profile, it will get a profile to
     * download.
     */
    fun activateEntry(simEntry: SimEntry) {
        // XXX TBD
    }
}
