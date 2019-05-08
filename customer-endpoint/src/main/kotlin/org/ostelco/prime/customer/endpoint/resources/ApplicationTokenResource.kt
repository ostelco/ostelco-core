package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Identity
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

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun storeApplicationToken(@Auth authToken: AccessTokenPrincipal?,
                              @NotNull applicationToken: ApplicationToken): Response {
        if (authToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getCustomer(identity = Identity(id = authToken.name, type = "EMAIL", provider = authToken.provider))
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { customer ->
                            dao.storeApplicationToken(customer.id, applicationToken).fold(
                                    { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                                    { Response.status(Response.Status.CREATED).entity(asJson(it)) })
                        })
                .build()
    }
}
