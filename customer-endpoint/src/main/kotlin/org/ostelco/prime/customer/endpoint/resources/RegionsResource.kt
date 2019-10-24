package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.responseBuilder
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
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
    fun getRegions(@Auth token: AccessTokenPrincipal?): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.getRegions(identity = token.identity)
                        .responseBuilder()
            }.build()

    @GET
    @Path("/{regionCode}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getRegion(@Auth token: AccessTokenPrincipal?,
                  @NotNull
                  @PathParam("regionCode")
                  regionCode: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.getRegion(
                        identity = token.identity,
                        regionCode = regionCode)
                        .responseBuilder()
            }.build()

    @Path("/{regionCode}/kyc")
    fun kycResource(
            @NotNull
            @PathParam("regionCode")
            regionCode: String
    ): KycResource = when (regionCode.toLowerCase()) {
        "sg" -> SingaporeKycResource(dao = dao)
        "my" -> MalaysiaKycResource(dao = dao)
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