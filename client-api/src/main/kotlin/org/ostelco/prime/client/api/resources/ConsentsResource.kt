package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Identity

import javax.validation.constraints.NotNull
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Consents API.
 */
@Path("/consents")
class ConsentsResource(private val dao: SubscriberDAO) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getConsents(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getConsents(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider))
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @PUT
    @Path("{consent-id}")
    @Produces(MediaType.APPLICATION_JSON)
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
            dao.acceptConsent(
                    identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                    consentId = consentId)
        } else {
            dao.rejectConsent(
                    identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                    consentId = consentId)
        }

        return result.fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }
}
