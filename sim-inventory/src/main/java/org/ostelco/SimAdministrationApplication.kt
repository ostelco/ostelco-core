package org.ostelco

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.hibernate.validator.constraints.NotEmpty
import java.io.BufferedReader
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import javax.ws.rs.Consumes


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

    override fun run(configuration: SimAdministrationAppConfiguration,
                     environment: Environment) {

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

        environment.jersey().register(EsimInventoryResource())
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

/**
 *  Representing a single SIM card.
 */
// @JsonSchema("SimEntry")
data class SimEntry(
        @JsonProperty("id") val id: Long,
        @JsonProperty("batchId") val batchId: Long = 99L,
        @JsonProperty("hlrId") val hlrId: String,
        @JsonProperty("smdpplus") val smdpplus: String?=null,
        @JsonProperty("msisdn") val msisdn: String? = null,
        @JsonProperty("iccid") val iccid: String,
        @JsonProperty("imsi") val imsi: String,
        @JsonProperty("eid") val eid: String? = null,
        @JsonProperty("active") val active: Boolean,
        @JsonProperty("pin1") val pin1: String,
        @JsonProperty("pin2") val pin2: String,
        @JsonProperty("puk1") val puk1: String,
        @JsonProperty("puk2") val puk2: String
)


/**
 * Describe a batch of SIM cards that was imported at some time
 */
data class SimImportBatch(
        @JsonProperty("id") val id: Long,
        @JsonProperty("startedAt") val startedAt: String,
        @JsonProperty("endedAt") val endedAt: String,
        @JsonProperty("successfull") val successful: Boolean,
        @JsonProperty("message") val status: String?,
        @JsonProperty("importer") val importer: String,
        @JsonProperty("size") val size: Long,
        @JsonProperty("hlr") val hlr: String,
        @JsonProperty("profileVendor") val profileVendor: String
        )


// XXX Need to register profileVendor into some registry, also need to
//     remember import batches, and a registry of entities that
//     are permitted to entre profiles.  So we need entity classes
//     for those and DAOs that permits us to talk about them in a
//     "stateless" manner.

///
///  The web resource using the protocol domain model.
///

@Path("/ostelco/sim-inventory/{hlr}/")
class EsimInventoryResource() {

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)

    @Path("iccid/{iccid}")
    @GET
    fun findByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String):SimEntry {
            return SimEntry(
                    id = 1L,
                    hlrId = "foo",
                    iccid = iccid,
                    imsi = "foo",
                    eid  = "bb",
                    active = false,
                    pin1 = "ss",
                    pin2 = "ss",
                    puk1 = "ss",
                    puk2 = "ss"
            )
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)

    @Path("imsi/{imsi}")
    @GET
    fun findByImsi(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("imsi") imsi: String):SimEntry {
        return SimEntry(
                id = 1L,
                hlrId = "foo",
                iccid =" a",
                imsi = imsi,
                eid  = "bb",
                active = false,
                pin1 = "ss",
                pin2 = "ss",
                puk1 = "ss",
                puk2 = "ss"
        )
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)

    @Path("msisdn/{msisdn}/allocate")
    @GET
    fun allocateNextFree(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("msisdn") msisdn: String):SimEntry {
        return SimEntry(
                id = 1L,
                msisdn=msisdn,
                hlrId = "foo",
                iccid =" a",
                imsi = "9u8y32879329783247",
                eid  = "bb",
                active = false,
                pin1 = "ss",
                pin2 = "ss",
                puk1 = "ss",
                puk2 = "ss"
        )
    }

    // XXX Arguably this shouldn't be done synchronously since it
    //     may take a long time.
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)

    @Path("/iccid/{iccid}/activate/all")
    @GET
    fun activateByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String):SimEntry {
        return SimEntry(
                id = 1L,
                msisdn="82828282828282828",
                hlrId = "foo",
                iccid = iccid,
                imsi = "9u8y32879329783247",
                eid  = "bb",
                active = false,
                pin1 = "ss",
                pin2 = "ss",
                puk1 = "ss",
                puk2 = "ss"
        )
    }


    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)


    @Path("/iccid/{iccid}/activate/hlr")
    @GET
    fun activateProfileByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String):SimEntry {
        return SimEntry(
                id = 1L,
                msisdn="82828282828282828",
                hlrId = "foo",
                iccid = iccid,
                imsi = "9u8y32879329783247",
                eid  = "bb",
                active = false,
                pin1 = "ss",
                pin2 = "ss",
                puk1 = "ss",
                puk2 = "ss"
        )
    }


    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)

    @Path("/iccid/{iccid}/activate/esim")
    @GET
    fun activateEsimProfileByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String):SimEntry {
        return SimEntry(
                id = 1L,
                msisdn="82828282828282828",
                hlrId = "foo",
                iccid = iccid,
                imsi = "9u8y32879329783247",
                eid  = "bb",
                active = false,
                pin1 = "ss",
                pin2 = "ss",
                puk1 = "ss",
                puk2 = "ss"
        )
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)

    @Path("/iccid/{iccid}/deactivate/hlr")
    @GET
    fun deactrivateByIccid(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("iccid") iccid: String):SimEntry {
        return SimEntry(
                id = 1L,
                msisdn="2134234",
                hlrId = "foo",
                iccid = iccid,
                imsi = "9u8y32879329783247",
                eid  = "bb",
                active = false,
                pin1 = "ss",
                pin2 = "ss",
                puk1 = "ss",
                puk2 = "ss"
        )
    }


    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Throws(IOException::class)
    @Path("/import-batch/sim-profile-vendor/{profilevendor}")
    @PUT
    fun importBatch(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @PathParam("profilevendor") profilevendor: String,
            csv: InputStream): SimImportBatch {
        // XXX A bunch of parameters are ignored here.
        return SimImportBatchReader(csv).read()
    }
}


// XXX This class started out as a CSV reader of bank records, and has not
// yet been fully transformed into a reader of a CSV with ICCID profiles
// as they arrive from the SIM Profile factory.

class SimImportBatchReader(val csvInputStream: InputStream) {

    fun read(): SimImportBatch {

        // XXX Adjust to fit whatever format we should cater to, there may
        //     be some variation between  sim vendors, and that should be
        //     something we can adjust to given the parameters sent to the
        //     reader class on creation.   Should anyway be configurable in
        //     a config file or perhaps some config database.
        val csvFileFormat = CSVFormat.DEFAULT
                .withQuote(null)
                .withIgnoreEmptyLines(true)
                .withTrim()
                .withDelimiter(';')

        val records = mutableListOf<CSVRecord>()
        BufferedReader(InputStreamReader(csvInputStream, Charset.forName(
                        "ISO-8859-1"))).use { reader ->
            CSVParser(reader, csvFileFormat).use { csvParser ->
                for (csvRecord in csvParser) {
                    println(csvRecord)
                }
            }
        }


        return SimImportBatch(
                id = 1L,
                status="wtf",
                startedAt =  "13123",
                endedAt =  "999",
                successful = true,
                importer = "mmm",
                size = 3,
                hlr="loltel",
                profileVendor = "idemia"
        )
    }
}
