package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.model.ApplicationToken
import javax.validation.constraints.NotNull
import javax.ws.rs.*
import javax.ws.rs.core.Response

/**
 * ApplicationToken API.
 *
 */
@Path("/applicationtoken")
class ApplicationTokenResource(private val dao: SubscriberDAO) : ResourceHelpers() {

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    fun storeApplicationToken(@Auth authToken: AccessTokenPrincipal?,
                      @NotNull applicationToken: ApplicationToken): Response {
        if (authToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val msisdn = dao.getMsisdn(authToken.name)

        if (msisdn.isRight) {
            val m = msisdn.right().get()
            println("ApplicationTokenResource called with msisdn : $m")
        } else {
            println("ApplicationTokenResource could not find subscriper msisdn")
        }

        val result = dao.getSubscriptionStatus(authToken.name)

        return if (result.isRight) {
            Response.status(Response.Status.CREATED)
                    .entity(asJson(result.right().get()))
                    .build()
        } else {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(asJson(result.left().get()))
                    .build()
        }
    }
}
