package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO

import javax.validation.constraints.NotNull
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

/**
 * Consents API.
 */
@Path("/consents")
class ConsentsResource(private val dao: SubscriberDAO) : ResourceHelpers() {

    @GET
    @Produces("application/json")
    fun getConsents(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val result = dao.getConsents(token.name)

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

    @PUT
    @Path("{consent-id}")
    @Produces("application/json")
    fun updateConsent(@Auth token: AccessTokenPrincipal?,
                      @NotNull
                      @PathParam("consent-id")
                      consentId: String,
                      @DefaultValue("true") @QueryParam("accepted") accepted: Boolean): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val result = if (accepted) {
            dao.acceptConsent(token.name, consentId)
        } else {
            dao.rejectConsent(token.name, consentId)
        }

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
}
