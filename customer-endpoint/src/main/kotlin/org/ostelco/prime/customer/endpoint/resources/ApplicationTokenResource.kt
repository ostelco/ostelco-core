package org.ostelco.prime.customer.endpoint.resources

import arrow.core.flatMap
import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.responseBuilder
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.tracing.EnableTracing
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * ApplicationToken API.
 *
 */
@Path("/applicationToken")
class ApplicationTokenResource(private val dao: SubscriberDAO) {

    @EnableTracing
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun storeApplicationToken(@Auth authToken: AccessTokenPrincipal?,
                              @NotNull applicationToken: ApplicationToken): Response =
            if (authToken == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.getCustomer(identity = authToken.identity)
                        .flatMap { customer ->
                            dao.storeApplicationToken(customer.id, applicationToken)
                        }
                        .responseBuilder(Response.Status.CREATED)
            }.build()
}
