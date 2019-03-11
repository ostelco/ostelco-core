package org.ostelco.simcards.inventory

import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.apierror.ApiErrorMapper.mapSimManagerErrorToApiError
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.simmanager.SimManagerError
import java.io.IOException
import java.io.InputStream
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


///
///  The web resource using the protocol domain model.
///

@Path("/ostelco/sim-inventory/{hlrVendors}")
class SimInventoryResource(private val api: SimInventoryApi) {

    @GET
    @Path("profileStatusList/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getSimProfileStatus(
            @NotEmpty @PathParam("hlrVendors") hlrName: String,
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
            @NotEmpty @PathParam("hlrVendors") hlrName: String,
            @NotEmpty @PathParam("iccid") iccid: String): Response =
            api.findSimProfileByIccid(hlrName, iccid)
                    .fold(
                            {
                                error("Failed to find SIM profile for BSS: ${hlrName} and ICCID: ${iccid}",
                                        ApiErrorCode.FAILED_TO_FETCH_SIM_PROFILE, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()

    @POST
    @Path("iccid/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun activateHlrProfileByIccid(
            @NotEmpty @PathParam("hlrVendors") hlrName: String,
            @NotEmpty @PathParam("iccid") iccid: String): Response =
            api.activateHlrProfileByIccid(hlrName, iccid)
                    .fold(
                            {
                                error("Failed to activate SIM profile with BSS ${hlrName} for ICCID: ${iccid}",
                                        ApiErrorCode.FAILED_TO_ACTIVATE_SIM_PROFILE_WITH_HLR, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()


    @DELETE
    @Path("iccid/{iccid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun deactivateHlrProfileByIccid(
            @NotEmpty @PathParam("hlrVendors") hlrName: String,
            @NotEmpty @PathParam("iccid") iccid: String): Response =
            api.deactivateHlrProfileByIccid(hlrName, iccid)
                    .fold(
                            {
                                error("Failed to deactivate SIM profile with BSS ${hlrName} for ICCID: ${iccid}",
                                        ApiErrorCode.FAILED_TO_DEACTIVATE_SIM_PROFILE_WITH_HLR, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()

    @GET
    @Path("imsi/{imsi}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByImsi(
            @NotEmpty @PathParam("hlrVendors") hlrName: String,
            @NotEmpty @PathParam("imsi") imsi: String): Response =
            api.findSimProfileByImsi(hlrName, imsi)
                    .fold(
                            {
                                error("Failed to find SIM profile for BSS: ${hlrName} and IMSI: ${imsi}",
                                        ApiErrorCode.FAILED_TO_FETCH_PROFILE, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()

    @GET
    @Path("msisdn/{msisdn}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSimProfileByMsisdn(
            @NotEmpty @PathParam("hlrVendors") hlrName: String,
            @NotEmpty @PathParam("msisdn") msisdn: String): Response =
            api.findSimProfileByMsisdn(hlrName, msisdn)
                    .fold(
                            {
                                error("Failed to find SIM profile for BSS: ${hlrName} and MSISDN: ${msisdn}",
                                        ApiErrorCode.FAILED_TO_FETCH_PROFILE, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()

    @POST
    @Path("esim")
    @Produces(MediaType.APPLICATION_JSON)
    fun activateNextEsimProfile(
            @NotEmpty @PathParam("hlrVendors") hlrName: String,
            @DefaultValue("_") @QueryParam("phoneType") phoneType: String): Response =
            api.activateNextEsimProfile(hlrName, phoneType)
                    .fold(
                            {
                                error("Failed to activate SIM profile with BSS ${hlrName}",
                                        ApiErrorCode.FAILED_TO_ACTIVATE_SIM_PROFILE, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()

    @GET
    @Path("esim")
    @Produces(MediaType.APPLICATION_JSON)
    fun allocateNextEsimProfile(
            @NotEmpty @PathParam("hlrVendors") hlrName: String,
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
            @NotEmpty @PathParam("hlrVendors") hlrName: String,
            @NotEmpty @PathParam("simVendor") simVendor: String,
            csvInputStream: InputStream): Response =
            api.importBatch(hlrName, simVendor, csvInputStream)
                    .fold(
                            {
                                error("Failed to upload batch with SIM profiles for BSS ${hlrName} and SIM profile vendor ${simVendor}",
                                        ApiErrorCode.FAILED_TO_IMPORT_BATCH, it)
                            },
                            { Response.status(Response.Status.OK).entity(asJson(it)) }
                    ).build()

    /* Maps internal errors to format suitable for HTTP/REST. */
    private fun error(description: String, code: ApiErrorCode, error: SimManagerError): Response.ResponseBuilder {
        val apiError = mapSimManagerErrorToApiError(description, code, error)
        return Response.status(apiError.status).entity(asJson(apiError))
    }
}
