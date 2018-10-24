package org.ostelco.prime.paymentprocessor.resources

import com.google.gson.JsonSyntaxException
import com.stripe.Stripe
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.net.Webhook
import org.ostelco.prime.getLogger
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

/**
 * Webhook service for handling Stripe event reports.
 *
 */
@Path("/stripe/event")
class StripeWebhookResource {

    private val logger by getLogger()

    @POST
    @Produces("application/json")
    fun handleEvent(@NotNull @Valid @HeaderParam("Stripe-Signature")
                    signature: String,
                    @NotNull @Valid
                    payload: String): Response {

        val event: Event = try {
            Webhook.constructEvent(payload, signature, Stripe.apiKey)
        } catch (e: JsonSyntaxException) {
            logger.error("Invalid payload in Stripe event ${e}")
            return Response.status(Response.Status.BAD_REQUEST)
                    .build()
        } catch (e: SignatureVerificationException) {
            logger.error("Invalid signature for Stripe event ${e}")
            return Response.status(Response.Status.BAD_REQUEST)
                    .build()
        }

        logger.info(NOTIFY_OPS_MARKER, "Got Stripe event ${event.type}")

        return Response.status(Response.Status.OK)
                .build()
    }
}