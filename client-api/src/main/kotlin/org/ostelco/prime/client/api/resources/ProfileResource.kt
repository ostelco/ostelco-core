package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

/**
 * Profile API.
 *
 */
@Path("/profile")
class ProfileResource(private val dao: SubscriberDAO) : ResourceHelpers() {

    private val paymentProcessor by lazy { getResource<PaymentProcessor>() }

    @GET
    @Produces("application/json")
    fun getProfile(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val result = dao.getProfile(token.name)

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
    @Produces("application/json")
    @Consumes("application/json")
    fun createProfile(@Auth token: AccessTokenPrincipal?,
                      @NotNull profile: Subscriber): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val result = dao.createProfile(token.name, profile)

        return if (result.isRight) {
            Response.status(Response.Status.CREATED)
                    .entity(asJson(result.right().get()))
                    .build()
        } else {
            Response.status(Response.Status.FORBIDDEN)
                    .entity(asJson(result.left().get()))
                    .build()
        }
    }

    @PUT
    @Produces("application/json")
    @Consumes("application/json")
    fun updateProfile(@Auth token: AccessTokenPrincipal?,
                      @NotNull profile: Subscriber): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val result = dao.updateProfile(token.name, profile)

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
