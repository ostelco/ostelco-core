package org.ostelco.prime.paymentprocessor.resources

import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import arrow.core.right
import com.stripe.model.Event
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorMapper.mapPaymentErrorToApiError
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.StripeEventState
import org.ostelco.prime.paymentprocessor.StripeMonitor
import org.ostelco.prime.paymentprocessor.core.BadGatewayError
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.publishers.StripeEventPublisher
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/stripe/monitor")
@Produces(MediaType.APPLICATION_JSON)
class StripeMonitorResource(val monitor: StripeMonitor) {

    private val logger by getLogger()

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
    @Path("webhook/enabled")
    fun checkWebhookEnabled(@QueryParam("url")
                     url: String? = null): Response =
            (if (url != null)
                monitor.checkWebhookEnabled(url)
            else
                monitor.checkWebhookEnabled())
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_WEBHOOK_ENABLED) },
                            { ok(it) }
                    ).build()

    @GET
    @Path("webhook/events/subscribed")
    fun getSubscribedToEvents(@QueryParam("url")
                     url: String? = null): Response =
            (if (url != null)
                monitor.checkWebhookEnabled(url)
            else
                monitor.checkWebhookEnabled())
                    .flatMap {
                        monitor.fetchEventSubscriptionList()
                    }
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_FETCH_SUBSCRIBED_TO_EVENTS) },
                            { ok(it) }
                    ).build()

    @GET
    @Path("events/failed")
    fun checkEventsNotDelivered(): Response =
            monitor.fetchEvents(limit = 1,
                    state = StripeEventState.FAILED_TO_DELIVER)
                    .flatMap {
                        it.isNotEmpty().right()
                    }
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_EVENTS_NOT_DELIVERED) },
                            { ok(it) }
                    ).build()

    @GET
    @Path("events/fetch/failed")
    fun fetchUndeliveredEvents(@QueryParam("interval")
                               interval: Long = 7200L): Response =
            monitor.fetchEvents(after = interval,
                    before = 0L,
                    state = StripeEventState.FAILED_TO_DELIVER)
                    .flatMap {
                        publishFailedEvents(it)
                    }
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_EVENTS_NOT_DELIVERED) },
                            { ok(it) }
                    ).build()

    /* Will actually never return an error as errors are swallowed, but
       logged, by the 'publisher'. */
    private fun publishFailedEvents(events: List<Event>): Either<PaymentError, Int> =
            Try {
                events.forEach {
                    publisher.publish(it)
                }
                events.size
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

    private fun ok(cnt: Int) =
            Response.status(Response.Status.OK).entity(asJson(cnt))

    private fun ok(lst: List<String>) =
            Response.status(Response.Status.OK).entity(asJson(lst))

    private fun failed(error: PaymentError, code: ApiErrorCode): Response.ResponseBuilder =
            mapPaymentErrorToApiError(description = error.description,
                    errorCode = code,
                    paymentError = error).let {
                Response.status(it.status).entity(asJson(it))
            }
}
