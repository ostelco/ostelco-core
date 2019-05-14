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

@Path("/ostelco/sim-inventory/{hssVendors}")
class SimInventoryResource(private val api: SimInventoryApi) {

    @GET
    @Path("profileStatusList/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getSimProfileStatus(
            @NotEmpty @PathParam("hssVendors") hlrName: String,
            @NotEmpty @PathParam("iccid") iccid: String): Response =
            api.getSimProfileStatus(hlrName, iccid)
                    .fold(
                            {
                                error("Failed to fetch SIM profile from vendor for BSS: ${hlrName} and ICCID: ${iccid}",
                                        ApiErrorCode.FAILED_TO_FETCH_SIM_PROFILE, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()

    @GET
    @Path("iccid/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByIccid(
            @NotEmpty @PathParam("hssVendors") hlrName: String,
            @NotEmpty @PathParam("iccid") iccid: String): Response =
            api.findSimProfileByIccid(hlrName, iccid)
                    .fold(
                            {
                                error("Failed to find SIM profile for BSS: ${hlrName} and ICCID: ${iccid}",
                                        ApiErrorCode.FAILED_TO_FETCH_SIM_PROFILE, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()


    @GET
    @Path("imsi/{imsi}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByImsi(
            @NotEmpty @PathParam("hssVendors") hlrName: String,
            @NotEmpty @PathParam("imsi") imsi: String): Response =
            api.findSimProfileByImsi(hlrName, imsi)
                    .fold(
                            {
                                error("Failed to find SIM profile for BSS: ${hlrName} and IMSI: ${imsi}",
                                        ApiErrorCode.FAILED_TO_FETCH_SIM_PROFILE, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()

    @GET
    @Path("msisdn/{msisdn}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByMsisdn(
            @NotEmpty @PathParam("hssVendors") hlrName: String,
            @NotEmpty @PathParam("msisdn") msisdn: String): Response =
            api.findSimProfileByMsisdn(hlrName, msisdn)
                    .fold(
                            {
                                error("Failed to find SIM profile for BSS: ${hlrName} and MSISDN: ${msisdn}",
                                        ApiErrorCode.FAILED_TO_FETCH_SIM_PROFILE, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()


    @GET
    @Path("esim")
    @Produces(MediaType.APPLICATION_JSON)
    fun allocateNextEsimProfile(
            @NotEmpty @PathParam("hssVendors") hlrName: String,
            @DefaultValue("_") @QueryParam("phoneType") phoneType: String): Response =
            api.allocateNextEsimProfile(hlrName, phoneType)
                    .fold(
                            {
                                error("Failed to reserve SIM profile with BSS ${hlrName}",
                                        ApiErrorCode.FAILED_TO_RESERVE_ACTIVATED_SIM_PROFILE, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()

    @PUT
    @Path("import-batch/profilevendor/{simVendor}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Throws(IOException::class)
    fun importBatch(
            @NotEmpty @PathParam("hssVendors") hss: String,
            @NotEmpty @PathParam("simVendor") simVendor: String,
            @Context  context: UriInfo,
            @QueryParam("initialHssState") @DefaultValue("NOT_ACTIVATED")  initialHssState: HssState,
            csvInputStream: InputStream): Response {

        // Check for illegal query parameters.  We don't want _anything_ to be inexact when importing
        // sim profiles.  Typos have consequences, such as using a default value, when an override was
        // intended.
        val unknownQueryParameters = context.queryParameters.keys.subtract(listOf("initialHssState"))
        if (unknownQueryParameters.isNotEmpty()) {
            return Response.status(
                    Response.Status.BAD_REQUEST)
                    .entity("Unknown query parameter(s): \"${unknownQueryParameters.joinToString(separator = ", ")}\"")
                    .build()
        }

        return api.importBatch(hss, simVendor, csvInputStream, initialHssState)
                .fold(
                        {
                            error("Failed to upload batch with SIM profiles for HSS ${hss} and SIM profile vendor ${simVendor}",
                                    ApiErrorCode.FAILED_TO_IMPORT_BATCH, it)
                        },
                        { Response.status(Response.Status.OK).entity(asJson(it)) }
                ).build()
    }


    /* Maps internal errors to format suitable for HTTP/REST. */
    private fun error(description: String, code: ApiErrorCode, error: SimManagerError): Response.ResponseBuilder {
        val apiError = mapSimManagerErrorToApiError(description, code, error)
        return Response.status(apiError.status).entity(asJson(apiError))
    }
}
