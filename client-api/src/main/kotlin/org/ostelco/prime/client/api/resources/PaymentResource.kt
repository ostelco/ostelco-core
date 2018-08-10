package org.ostelco.prime.client.api.resources

import arrow.core.flatMap
import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

/**
 * Payment API.
 *
 */
@Path("/paymentSources")
class PaymentResource(private val dao: SubscriberDAO) {

    private val paymentProcessor by lazy { getResource<PaymentProcessor>() }

    @POST
    @Produces("application/json")
    fun createSource(@Auth token: AccessTokenPrincipal?,
                     @NotNull
                     @QueryParam("sourceId")
                     sourceId: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getPaymentProfile(token.name)
                .flatMap { profileInfo -> paymentProcessor.addSource(profileInfo.id, sourceId) }
                .fold(
                        { Response.status(Response.Status.BAD_GATEWAY).entity(asJson(it)) },
                        { Response.status(Response.Status.CREATED).entity(asJson(it)) }
                ).build()
    }


    @GET
    @Produces("application/json")
    fun listSources(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getPaymentProfile(token.name)
                .flatMap { profileInfo -> paymentProcessor.getSavedSources(profileInfo.id) }
                .fold(
                        { Response.status(Response.Status.BAD_GATEWAY).entity(asJson(it)) },
                        { Response.status(Response.Status.CREATED).entity(asJson(it)) }
                ).build()
    }

    @PUT
    fun setDefaultSource(@Auth token: AccessTokenPrincipal?,
                         @NotNull
                         @QueryParam("sourceId")
                         sourceId: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getPaymentProfile(token.name)
                .flatMap { profileInfo -> paymentProcessor.setDefaultSource(profileInfo.id, sourceId) }
                .fold(
                        { Response.status(Response.Status.BAD_GATEWAY).entity(asJson(it)) },
                        { Response.status(Response.Status.CREATED).entity(asJson(it)) }
                ).build()
    }
}
