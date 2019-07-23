package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.responseBuilder
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
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
                     sourceId: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.createSource(
                        identity = token.identity,
                        sourceId = sourceId)
                        .responseBuilder(Response.Status.CREATED)
            }.build()

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun listSources(@Auth token: AccessTokenPrincipal?): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.listSources(
                        identity = token.identity)
                        .responseBuilder()
            }.build()

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    fun setDefaultSource(@Auth token: AccessTokenPrincipal?,
                         @NotNull
                         @QueryParam("sourceId")
                         sourceId: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.setDefaultSource(
                        identity = token.identity,
                        sourceId = sourceId)
                        .responseBuilder()
            }.build()

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    fun removeSource(@Auth token: AccessTokenPrincipal?,
                     @NotNull
                     @QueryParam("sourceId")
                     sourceId: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.removeSource(
                        identity = token.identity,
                        sourceId = sourceId)
                        .responseBuilder()
            }.build()
}
