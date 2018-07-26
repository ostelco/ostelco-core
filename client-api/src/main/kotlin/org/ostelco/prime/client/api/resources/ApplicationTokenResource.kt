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

        val result = dao.getMsisdn(authToken.name)

        if (result.isRight) {
            val msisdn = result.right().get()
            val created = dao.storeApplicationToken(msisdn, applicationToken)
            if (created.isRight) {
                return Response.status(Response.Status.CREATED)
                        .entity(asJson(created.right().get()))
                        .build()
            } else {
                return Response.status(507) // Insufficient Storage
                        .entity(asJson(created.left().get()))
                        .build()
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(asJson(result.left().get()))
                    .build()
        }
    }
}
