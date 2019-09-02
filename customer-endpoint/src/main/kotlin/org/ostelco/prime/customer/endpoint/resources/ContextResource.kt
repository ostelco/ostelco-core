package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.responseBuilder
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.tracing.EnableTracing
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/context")
class ContextResource(private val dao: SubscriberDAO) {

    @EnableTracing
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getBundles(@Auth token: AccessTokenPrincipal?): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.getContext(identity = token.identity)
                        .responseBuilder()
            }.build()
}