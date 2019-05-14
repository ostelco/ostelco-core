package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Identity
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/regions")
class RegionsResource(private val dao: SubscriberDAO) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getRegions(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getRegions(identity = Identity(id = token.name, type = "EMAIL", provider = token.provider))
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @GET
    @Path("/{regionCode}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getRegion(
            @Auth token: AccessTokenPrincipal?,
            @NotNull
            @PathParam("regionCode")
            regionCode: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getRegion(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                regionCode = regionCode)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @Path("/{regionCode}/kyc")
    fun kycResource(
            @NotNull
            @PathParam("regionCode")
            regionCode: String
    ): KycResource = when (regionCode.toLowerCase()) {
        "sg" -> SingaporeKycResource(dao = dao)
        else -> KycResource(regionCode = regionCode, dao = dao)
    }

    @Path("/{regionCode}/subscriptions")
    fun subscriptionsResource(
            @NotNull
            @PathParam("regionCode")
            regionCode: String
    ): SubscriptionsResource = SubscriptionsResource(regionCode = regionCode, dao = dao)

    @Path("/{regionCode}/simProfiles")
    fun simProfilesResource(
            @NotNull
            @PathParam("regionCode")
            regionCode: String
    ): SimProfilesResource = SimProfilesResource(regionCode = regionCode, dao = dao)
}