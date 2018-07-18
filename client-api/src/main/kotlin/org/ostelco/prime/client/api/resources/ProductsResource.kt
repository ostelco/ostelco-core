package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.core.ApiError
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import io.vavr.control.Either
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

        val paymentProfile = getOrCreatePaymentProfile(token.name)

        if (paymentProfile.isLeft) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(asJson(paymentProfile.left().get()))
                    .build()
        }

        val product = dao.getProduct(token.name, sku)

        if (product.isLeft) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(asJson(product.left().get()))
                    .build()
        }

        val customerId = paymentProfile.right().get().id
        val price = product.right().get().price

        val result = paymentProcessor.purchaseProduct(customerId, sourceId, price.amount,
                            price.currency, saveCard)

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

    private fun getOrCreatePaymentProfile(name: String): Either<ApiError, ProfileInfo> {
        val profile = dao.getPaymentProfile(name)

        return if (profile.isRight) {
            profile
        } else {
            createAndStorePaymentProfile(name)
        }
    }

    private fun createAndStorePaymentProfile(name: String): Either<ApiError, ProfileInfo> {
        val profile = paymentProcessor.createPaymentProfile(name)

        if (profile.isRight) {
            val error = dao.setPaymentProfile(name, profile.right().get())
            if (!error.isEmpty) {
                /* TODO: Remove profile with payment-processor. */
                return Either.left(error.get())
            }
        }
        return profile
    }
}
