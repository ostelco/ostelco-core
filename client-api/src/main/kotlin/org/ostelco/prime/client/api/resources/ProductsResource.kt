package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

/**
 * Products API.
 *
 */
@Path("/products")
class ProductsResource(private val dao: SubscriberDAO) : ResourceHelpers() {

    private val paymentProcessor by lazy { getResource<PaymentProcessor>() }

    @GET
    @Produces("application/json")
    fun getProducts(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val result = dao.getProducts(token.name)

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

    @POST
    @Path("{sku}")
    @Produces("application/json")
    fun purchaseProduct(@Auth token: AccessTokenPrincipal?,
                        @NotNull
                        @PathParam("sku")
                        sku: String,
                        @NotNull
                        @QueryParam("sourceId")
                        sourceId: String,
                        @QueryParam("saveCard")
                        saveCard: Boolean): Response {    /* 'false' is default. */
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val customerId: String? = dao.getCustomerId(token.name)
                ?: paymentProcessor.createProfile(token.name)

        if (customerId == null) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .build()
        }

        val result = dao.createProfile(token.name, customerId)

        if (result.isLeft) {
            // Remove payment registration
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(asJson(result.left().get()))
                    .build()
        }

        val product: Product = dao.getProduct(sku)
                ?: return Response.status(Response.Status.NOT_FOUND).build()

        val result = paymentProcessor.purchaseProduct(customerId, sourceId, product, saveCard)

        return if (result.isRight) {
            Response.status(Response.Status.CREATED)
                .entity(asJson(result.right().get()))
                .build()
        } else {
            Response.status(Response.Status.BAD_GATEWAY)
                    .entity(asJson(result.left().get()))
                    .build()
        }
    }
}
