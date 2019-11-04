package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.responseBuilder
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.tracing.EnableTracing
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Products API.
 *
 */
@Path("/products")
class ProductsResource(private val dao: SubscriberDAO) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getProducts(@Auth token: AccessTokenPrincipal?): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.getProducts(identity = token.identity)
                        .responseBuilder()
            }.build()

    @EnableTracing
    @POST
    @Path("{sku}/purchase")
    @Produces(MediaType.APPLICATION_JSON)
    fun purchaseProduct(@Auth token: AccessTokenPrincipal?,
                        @NotNull
                        @PathParam("sku")
                        sku: String,
                        @QueryParam("sourceId")
                        sourceId: String?,
                        @QueryParam("saveCard")
                        saveCard: Boolean?): Response =    /* 'false' is default. */
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                dao.purchaseProduct(
                        identity = token.identity,
                        sku = sku,
                        sourceId = sourceId,
                        saveCard = saveCard ?: false)
                        .responseBuilder(Response.Status.CREATED)
            }.build()

    @POST
    @Path("{sku}/renew")
    @Produces(MediaType.APPLICATION_JSON)
    fun renewSubscription(@Auth token: AccessTokenPrincipal?,
                          @NotNull
                          @PathParam("sku")
                          sku: String,
                          @QueryParam("sourceId")
                          sourceId: String?,
                          @QueryParam("saveCard")
                          saveCard: Boolean?): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                if (sourceId == null) {
                    dao.renewSubscriptionToPlan(
                            identity = token.identity,
                            sku = sku)
                } else {
                    dao.renewSubscriptionToPlan(
                            identity = token.identity,
                            sku = sku,
                            sourceId = sourceId,
                            saveCard = saveCard ?: false)
                }.responseBuilder(Response.Status.CREATED)
            }.build()
}
