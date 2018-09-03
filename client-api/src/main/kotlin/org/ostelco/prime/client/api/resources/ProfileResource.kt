package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.model.Subscriber
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

/**
 * Profile API.
 *
 */
@Path("/profile")
class ProfileResource(private val dao: SubscriberDAO) {

    @GET
    @Produces("application/json")
    fun getProfile(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getProfile(token.name).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError.description)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    fun createProfile(@Auth token: AccessTokenPrincipal?,
                      @NotNull profile: Subscriber,
                      @QueryParam("referred_by") referredBy: String?): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.createProfile(token.name, profile, referredBy).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError.description)) },
                { Response.status(Response.Status.CREATED).entity(asJson(it)) })
                .build()
    }

    @PUT
    @Produces("application/json")
    @Consumes("application/json")
    fun updateProfile(@Auth token: AccessTokenPrincipal?,
                      @NotNull profile: Subscriber): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.updateProfile(token.name, profile).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError.description)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }
}
