package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.model.ApplicationToken
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

/**
 * ApplicationToken API.
 *
 */
@Path("/applicationtoken")
class ApplicationTokenResource(private val dao: SubscriberDAO) {

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    fun storeApplicationToken(@Auth authToken: AccessTokenPrincipal?,
                              @NotNull applicationToken: ApplicationToken): Response {
        if (authToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getMsisdn(authToken.name).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError.description)) },
                { msisdn ->
                    dao.storeApplicationToken(msisdn, applicationToken).fold(
                            { apiError -> Response.status(apiError.status).entity(asJson(apiError.description)) },
                            { Response.status(Response.Status.CREATED).entity(asJson(it)) })
                })
                .build()
    }
}
