package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response
import javax.ws.rs.client.Client

/**
 * Subscriptions API.
 *
 */
@Path("/subscription")
class SubscriptionResource(private val dao: SubscriberDAO,
                           val client: Client,
                           val pseudonymEndpoint: String) : ResourceHelpers() {

    @GET
    @Path("status")
    @Produces("application/json")
    fun getSubscription(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val result = dao.getSubscriptionStatus(token.name)

        return if (result.isRight) {
            Response.status(Response.Status.OK)
                    .entity(asJson(result.right().get()))
                    .build()
        } else {
            Response.status(Response.Status.NOT_FOUND)
                    .entity(asJson(result.left().get()))
                    .build()
        }
    }

    @GET
    @Path("activePseudonyms")
    @Produces("application/json")
    fun getActivePseudonyms(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val result = dao.getMsisdn(token.name)

        return if (result.isRight) {
            val msisdn = result.right().get()
            val target = client.target("${pseudonymEndpoint}/pseudonym/active/$msisdn")
            target.request().get()
        } else {
            Response.status(Response.Status.NOT_FOUND)
                    .entity(asJson(result.left().get()))
                    .build()
        }
    }
}
