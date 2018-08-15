package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

@Path("referred")
class ReferralResource(private val dao: SubscriberDAO) {

    @GET
    @Produces("application/json")
    fun getReferrals(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build()
        }

        return dao.getReferrals(token.name).fold(
                { Response.status(Response.Status.NOT_FOUND).entity(asJson(it)) },
                { Response.status(Response.Status.OK).entity(it) })
                .build()
    }

    @GET
    @Path("/by")
    @Produces("application/json")
    fun getReferredBy(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build()
        }

        return dao.getReferredBy(token.name).fold(
                { Response.status(Response.Status.NOT_FOUND).entity(asJson(it)) },
                { Response.status(Response.Status.OK).entity(it) })
                .build()
    }
}