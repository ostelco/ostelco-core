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

class Es2plusApplication : Application<Es2plusConfiguration>() {

    override fun getName(): String {
        return "SIM inventory application"
    }

    override fun initialize(bootstrap: Bootstrap<Es2plusConfiguration>) {
        // TODO: application initialization
    }

    override fun run(configuration: Es2plusConfiguration,
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
            Es2plusApplication().run(*args)
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


// @JsonSchema("SimEntry")
data class SimEntry(
        @JsonProperty("id") val id: Long,
        @JsonProperty("hlrId") val hlrId: String,
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

    @Path("allocate-next-free")
    @GET
    fun allocateNextFree(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @QueryParam("msisdn") msisdn: String):SimEntry {
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

    @Path("/iccid/{iccid}/activate")
    @GET
    fun activate(
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
    fun deactrivate(
            @NotEmpty @PathParam("hlr") hlr: String,
            @NotEmpty @QueryParam("iccid") iccid: String):SimEntry {
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
}
