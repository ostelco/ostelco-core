package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Identity
import javax.validation.constraints.NotNull
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Payment API.
 *
 */
@Path("/paymentSources")
class PaymentSourcesResource(private val dao: SubscriberDAO) {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun createSource(@Auth token: AccessTokenPrincipal?,
                     @NotNull
                     @QueryParam("sourceId")
                     sourceId: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.createSource(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                sourceId = sourceId)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { sourceInfo -> Response.status(Response.Status.CREATED).entity(sourceInfo) })
                .build()
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun listSources(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        return dao.listSources(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider))
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { sourceList -> Response.status(Response.Status.OK).entity(sourceList) })
                .build()
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    fun setDefaultSource(@Auth token: AccessTokenPrincipal?,
                         @NotNull
                         @QueryParam("sourceId")
                         sourceId: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.setDefaultSource(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                sourceId = sourceId)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { sourceInfo -> Response.status(Response.Status.OK).entity(sourceInfo) })
                .build()
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    fun removeSource(@Auth token: AccessTokenPrincipal?,
                     @NotNull
                     @QueryParam("sourceId")
                     sourceId: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.removeSource(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                sourceId = sourceId)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { sourceInfo -> Response.status(Response.Status.OK).entity(sourceInfo) })
                .build()
    }
}
