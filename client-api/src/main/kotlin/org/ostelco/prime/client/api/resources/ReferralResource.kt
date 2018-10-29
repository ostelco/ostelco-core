package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("referred")
class ReferralResource(private val dao: SubscriberDAO) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getReferrals(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build()
        }

        return dao.getReferrals(token.name).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(it) })
                .build()
    }

    @GET
    @Path("/by")
    @Produces(MediaType.APPLICATION_JSON)
    fun getReferredBy(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build()
        }

        return dao.getReferredBy(token.name).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(it) })
                .build()
    }
}
