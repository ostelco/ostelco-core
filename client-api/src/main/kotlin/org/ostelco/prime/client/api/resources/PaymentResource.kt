package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Response

/**
 * Payment API.
 *
 */
@Path("/paymentSources")
class PaymentResource(private val dao: SubscriberDAO) : ResourceHelpers() {

    private val paymentProcessor by lazy { getResource<PaymentProcessor>() }

    @POST
    @Consumes("application/json")
    fun listSources(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val paymentId: String = dao.getPaymentId(token.name)
                ?: return Response.status(Response.Status.NOT_FOUND).build()

        val sources = paymentProcessor.listSources(paymentId)

        return Response.status(Response.Status.OK)
                .entity(asJson(sources))
                .build()
    }
}
