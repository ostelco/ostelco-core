package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/bundles")
class BundlesResource(private val dao: SubscriberDAO) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getBundles(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getBundles(
                identity = token.identity)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }
}