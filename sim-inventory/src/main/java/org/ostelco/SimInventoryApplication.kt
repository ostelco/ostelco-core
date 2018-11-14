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
import org.hibernate.validator.constraints.NotEmpty
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import java.io.IOException
import java.io.InputStream
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
class SimInventoryApplication : Application<SimInventoryConfiguration>() {

    override fun getName(): String {
        return "SIM inventory application"
    }

    override fun initialize(bootstrap: Bootstrap<SimInventoryConfiguration>) {
        // TODO: application initialization
    }

    override fun run(configuration: SimInventoryConfiguration,
                     environment: Environment) {

        // XXX Add these parameters to configuration file.
        val oas = OpenAPI()
        val info = Info()
                .title(getName())
                .description("SIM inventory management.")
                .termsOfService("http://example.com/terms")
                .contact(Contact().email("rmz@redotter.com"))

        oas.info(info)
        val oasConfig = SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .resourcePackages(Stream.of("no.ostelco.org")
                        .collect(Collectors.toSet<String>()))
        environment.jersey().register(OpenApiResource()
                .openApiConfiguration(oasConfig))

        environment.jersey().register(EsimInventoryResource())
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SimInventoryApplication().run(*args)
        }
    }

    // We're basing this implementation on
    // https://www.gsma.com/newsroom/wp-content/uploads/SGP.22-v2.0.pdf
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
//     are permitted to entre profiles.




///
///  The web resource using the protocol domain model.
///

@Path("/ostelco/sim-inventory/{hlr}/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class EsimInventoryResource() {

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
    @Path("/iccid/{iccid}/activate")
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

    @Path("/iccid/{iccid}/deactivate")
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

        val csvFileFormat = CSVFormat.DEFAULT
                .withQuote(null)
                .withIgnoreEmptyLines(true)
                .withTrim()
                .withDelimiter(';')

        val records = mutableListOf<CSVRecord>()
        Files.newBufferedReader(
                Paths.get(csvFilePath),
                Charset.forName("ISO-8859-1")).use { reader ->
            CSVParser(reader, csvFileFormat).use { csvParser ->
                for (csvRecord in csvParser) {
                    records.add(csvRecord)
                }
            }
        }

        var i = 0;
        fun next(): CSVRecord {
            return records[i++]
        }

        fun getUnquotedField(field: CSVRecord, index: Int): String {
            return field[index].trim('"')
        }



        val transactions = mutableListOf<DnbTransactionRecord>()
        // Now we read all the records, while there are more
        while (i < records.size) {
            field = next()
            // XXX Read the PIN, PUK etc.
            val date = getUnquotedField(field, 0)
            val explanatoryText = getUnquotedField(field, 1)
            val status = getUnquotedField(field, 2)
            val transactionType = getUnquotedField(field, 3)
            val interestDate = getUnquotedField(field, 4)
            val incoming = getUnquotedField(field, 5)
            val outgoing = getUnquotedField(field, 6)
            val archiveRef = getUnquotedField(field, 7)
            val reference = getUnquotedField(field, 8)


            // XXX Shouldn't add to the list here, but
            //     should pass it on to the DAO, in a transaction.
            transactions.add(SimEntry(
                    date = date,
                    explanatoryText = explanatoryText,
                    status = status,
                    transactionType = transactionType,
                    interestDate = interestDate,
                    incoming = incoming,
                    emailAddress = emailAddress,
                    outgoing = outgoing,
                    archiveRef = archiveRef,
                    reference = reference))
        }
        return SimImportBatch(
                id = 1L,
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
