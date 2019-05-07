package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Identity
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
    fun getSimProfiles(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getSimProfiles(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                regionCode = regionCode)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun provisionSimProfile(
            @Auth token: AccessTokenPrincipal?,
            @QueryParam("profileType") profileType: String?): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.provisionSimProfile(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                regionCode = regionCode,
                profileType = profileType)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @PUT
    @Path("/{iccId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun updateSimProfile(
            @NotNull
            @PathParam("iccId")
            iccId: String,
            @NotNull
            @QueryParam("alias")
            alias: String,
            @Auth
            token: AccessTokenPrincipal?): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.updateSimProfile(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                regionCode = regionCode,
                iccId = iccId,
                alias = alias)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @GET
    @Path("/{iccId}/resendEmail")
    @Produces(MediaType.APPLICATION_JSON)
    fun sendEmailWithEsimActivationQrCode(
            @NotNull
            @PathParam("iccId")
            iccId: String,
            @Auth
            token: AccessTokenPrincipal?): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.sendEmailWithEsimActivationQrCode(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                regionCode = regionCode,
                iccId = iccId)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }
}
