package org.ostelco.prime.paymentprocessor.resources

import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import com.stripe.model.Event
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorMapper.mapPaymentErrorToApiError
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.ConfigRegistry
import org.ostelco.prime.paymentprocessor.StripeEventState
import org.ostelco.prime.paymentprocessor.StripeMonitor
import org.ostelco.prime.paymentprocessor.core.BadGatewayError
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.publishers.StripeEventPublisher
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response

@Path("/stripe/monitor")
class StripeMonitorResource() {

    private val logger by getLogger()

    private val monitor by lazy { getResource<StripeMonitor>() }
    private val publisher by lazy { getResource<StripeEventPublisher>() }

    @GET
    @Path("version")
    fun checkVersion(): Response =
            monitor.checkVersion()
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_API_VERSION) },
                            { ok(it) }
                    ).build()

    @GET
    @Path("webhook")
    fun checkWebhook(): Response =
            monitor.checkWebhookEnabled(ConfigRegistry.monitorConfig.webhookUrl)
                    .flatMap {
                        monitor.checkEventSubscriptionList(ConfigRegistry.monitorConfig.webhookEvents)
                    }
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_WEBHOOK) },
                            { ok(it) }
                    ).build()

    @GET
    @Path("events")
    fun checkFailedEvents(): Response =
            monitor.fetchEvents(after = epoch(-ConfigRegistry.monitorConfig.eventInterval),
                    before = 0L,
                    state = StripeEventState.FAILED_TO_DELIVER)
                    .flatMap {
                        publishFailedEvents(it)
                    }
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_EVENTS) },
                            { ok(it) }
                    ).build()

    /* Will actually never return an error as errors are swallowed, but
       logged, by the 'publisher'. */
    private fun publishFailedEvents(events: List<Event>): Either<PaymentError, Boolean> =
            Try {
                events.forEach {
                    publisher.publish(it)
                }
                true
            }.toEither {
                BadGatewayError("Failed to publish retrieved events")
            }

    /* Epoch timestamp, now or offset by +/- seconds. */
    private fun epoch(offset: Long = 0L): Long =
            LocalDateTime.now(ZoneOffset.UTC)
                    .plusSeconds(offset)
                    .atZone(ZoneOffset.UTC).toEpochSecond()

    private fun ok(status: Boolean) =
            Response.status(Response.Status.OK).entity(asJson(status))

    private fun failed(error: PaymentError, code: ApiErrorCode): Response.ResponseBuilder =
            mapPaymentErrorToApiError(description = error.description,
                    errorCode = code,
                    paymentError = error).let {
                Response.status(it.status).entity(asJson(it))
            }
}