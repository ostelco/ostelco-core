package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

/**
 * Products API.
 *
 */
@Path("/products")
class ProductsResource(private val dao: SubscriberDAO) {

    @GET
    @Produces("application/json")
    fun getProducts(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getProducts(token.name).fold(
                { Response.status(Response.Status.NOT_FOUND).entity(asJson(it)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @Deprecated("use purchaseProduct")
    @POST
    @Path("{sku}")
    @Produces("application/json")
    fun purchaseProductOld(@Auth token: AccessTokenPrincipal?,
                           @NotNull
                           @PathParam("sku")
                           sku: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.purchaseProduct(token.name, sku).fold(
                { Response.status(Response.Status.NOT_FOUND).entity(asJson(it)) },
                { Response.status(Response.Status.CREATED) })
                .build()
    }

    @POST
    @Path("{sku}/purchase")
    @Produces("application/json")
    fun purchaseProduct(@Auth token: AccessTokenPrincipal?,
                        @NotNull
                        @PathParam("sku")
                        sku: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.purchaseProduct(token.name, sku).fold(
                { Response.status(Response.Status.NOT_FOUND).entity(asJson(it)) },
                { Response.status(Response.Status.CREATED) })
                .build()
    }
}
