package org.ostelco.simcards.inventory

import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorMapper.mapSimManagerErrorToApiError
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.simmanager.SimManagerError
import java.io.IOException
import java.io.InputStream
import javax.ws.rs.Consumes
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo


/**
 * Web resource used for administrating SIM profiles from misc. vendors
 * for misc. HSSes.
 */

@Path("/ostelco/sim-inventory/{hssName}")
class SimInventoryResource(private val api: SimInventoryApi) {

    @GET
    @Path("profileStatusList/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getSimProfileStatus(
            @NotEmpty @PathParam("hssName") hssName: String,
            @NotEmpty @PathParam("iccid") iccid: String): Response =
            api.getSimProfileStatus(hssName, iccid)
                    .fold(
                            {
                                error("Failed to fetch SIM profile from vendor for BSS: $hssName and ICCID: $iccid",
                                        ApiErrorCode.FAILED_TO_FETCH_SIM_PROFILE, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()

    @GET
    @Path("iccid/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByIccid(
            @NotEmpty @PathParam("hssName") hssName: String,
            @NotEmpty @PathParam("iccid") iccid: String): Response =
            api.findSimProfileByIccid(hssName, iccid)
                    .fold(
                            {
                                error("Failed to find SIM profile for BSS: $hssName and ICCID: $iccid",
                                        ApiErrorCode.FAILED_TO_FETCH_SIM_PROFILE, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()

    @GET
    @Path("imsi/{imsi}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByImsi(
            @NotEmpty @PathParam("hssName") hssName: String,
            @NotEmpty @PathParam("imsi") imsi: String): Response =
            api.findSimProfileByImsi(hssName, imsi)
                    .fold(
                            {
                                error("Failed to find SIM profile for BSS: $hssName and IMSI: $imsi",
                                        ApiErrorCode.FAILED_TO_FETCH_SIM_PROFILE, it)
                            },
                            { simEntry -> Response.status(Response.Status.OK).entity(asJson(simEntry)) }
                    ).build()

    @GET
    @Path("msisdn/{msisdn}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByMsisdn(
            @NotEmpty @PathParam("hssName") hssName: String,
            @NotEmpty @PathParam("msisdn") msisdn: String): Response =
            api.findSimProfileByMsisdn(hssName, msisdn)
                    .fold(
                            {
                                error("Failed to find SIM profile for BSS: $hssName and MSISDN: $msisdn",
                                        ApiErrorCode.FAILED_TO_FETCH_SIM_PROFILE, it)
                            },
                            { simEntry -> Response.status(Response.Status.OK).entity(asJson(simEntry)) }
                    ).build()

    // XXX This method should also (like "importBatch)" be very strict with respect to query parameters. Only the
    //     explicitly listed ones should be permitted, otherwise the entire invocation should
    //     fail.
    @GET
    @Path("esim")
    @Produces(MediaType.APPLICATION_JSON)
    fun allocateNextEsimProfile(
            @NotEmpty @PathParam("hssName") hssName: String,
            @DefaultValue("_") @QueryParam("phoneType") phoneType: String): Response =
            api.allocateNextEsimProfile(hssName, phoneType)
                    .fold(
                            {
                                error("Failed to reserve SIM profile with BSS $hssName",
                                        ApiErrorCode.FAILED_TO_RESERVE_ACTIVATED_SIM_PROFILE, it)
                            },
                            { simEntry -> Response.status(Response.Status.OK).entity(asJson(simEntry)) }
                    ).build()

    @PUT
    @Path("import-batch/profilevendor/{simVendor}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Throws(IOException::class)
    fun importBatch(
            @NotEmpty @PathParam("hssName") hss: String,
            @NotEmpty @PathParam("simVendor") simVendor: String,
            @Context  context: UriInfo,
            @QueryParam("initialHssState") @DefaultValue("NOT_ACTIVATED")  initialHssState: HssState,
            csvInputStream: InputStream): Response {

        // Check for illegal query parameters.  We don't want _anything_ to be inexact when importing
        // sim profiles.  Typos have consequences, such as using a default value, when an override was
        // intended.
        // XXX This logic could/should be placed in an interceptor for an annotation (e.g. "@StrictQueryParameters")
        //     so that it a) don't clutter the method body with what is essentially a type safety concern.
        //                b) can more easily be reused by other methods with the same concern.
        val unknownQueryParameters = context.queryParameters.keys.subtract(listOf("initialHssState"))
        if (unknownQueryParameters.isNotEmpty()) {
            return Response.status(
                    Response.Status.BAD_REQUEST)
                    .entity("Unknown query parameter(s): \"${unknownQueryParameters.joinToString(separator = ", ")}\"")
                    .build()
        }

        // XXX This kind of marshalling/unmarshalling of either parameters clutters up the
        //     methods and makes the logic contained in them dificult to assess.
        //     This currently leads to repeated/verbose code and thus lower maintainability.
        //     An abstraction to unmarshal Arrow parameters into Response objects should be
        //     introduced.  It must be typed so that type safety can be checked by the compiler.
        //     currently the output of the importBatch is treated as a type-unsafe json
        //     blob, which is anathema to safe programming practice in a program with a proper
        //     type system.  Also, since the type is hidden from the return type via the Response mechanism,
        //     it is impossible to infer the type of the return object by observing the typing of the
        //     methods in the resource class. This makes both automatic documentation generation,
        //     automatic generation of OpenAPI specification, and automatic generation of client code
        //     based on the OpenAPI specification more difficult. All of these issues should
        //     be fixed.
        return api.importBatch(hss, simVendor, csvInputStream, initialHssState)
                .fold(
                        {
                            error("Failed to upload batch with SIM profiles for HSS $hss and SIM profile vendor $simVendor",
                                    ApiErrorCode.FAILED_TO_IMPORT_BATCH, it)
                        },
                        { simImportBatch -> Response.status(Response.Status.OK).entity(asJson(simImportBatch)) }
                ).build()
    }


    /* Maps internal errors to format suitable for HTTP/REST. */
    private fun error(description: String, code: ApiErrorCode, error: SimManagerError): Response.ResponseBuilder {
        val apiError = mapSimManagerErrorToApiError(description, code, error)
        return Response.status(apiError.status).entity(asJson(apiError))
    }
}
