package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.responseBuilder
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.tracing.EnableTracing
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class SimProfilesResource(private val regionCode: String, private val dao: SubscriberDAO) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getSimProfiles(@Auth token: AccessTokenPrincipal?): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.getSimProfiles(
                        identity = token.identity,
                        regionCode = regionCode)
                        .responseBuilder()
            }.build()

    @EnableTracing
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun provisionSimProfile(@Auth token: AccessTokenPrincipal?,
                            @QueryParam("profileType") profileType: String?): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.provisionSimProfile(
                        identity = token.identity,
                        regionCode = regionCode,
                        profileType = profileType)
                        .responseBuilder()
            }.build()

    @PUT
    @Path("/{iccId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun updateSimProfile(@NotNull
                         @PathParam("iccId")
                         iccId: String,
                         @NotNull
                         @QueryParam("alias")
                         alias: String,
                         @Auth
                         token: AccessTokenPrincipal?): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.updateSimProfile(
                        identity = token.identity,
                        regionCode = regionCode,
                        iccId = iccId,
                        alias = alias)
                        .responseBuilder()
            }.build()

    @EnableTracing
    @GET
    @Path("/{iccId}/resendEmail")
    @Produces(MediaType.APPLICATION_JSON)
    fun sendEmailWithEsimActivationQrCode(@NotNull
                                          @PathParam("iccId")
                                          iccId: String,
                                          @Auth
                                          token: AccessTokenPrincipal?): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.sendEmailWithEsimActivationQrCode(
                        identity = token.identity,
                        regionCode = regionCode,
                        iccId = iccId)
                        .responseBuilder()
            }.build()
}
