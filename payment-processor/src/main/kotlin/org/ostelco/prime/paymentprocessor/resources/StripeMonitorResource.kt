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
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
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
    @Path("apiversion")
    fun checkApiVersion(): Response =
            monitor.checkApiVersion()
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_API_VERSION) },
                            { ok(mapOf("match" to it)) }
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
                            { ok(mapOf("enabled" to it)) }
                    ).build()

    @GET
    @Path("webhook/events")
    fun getSubscribedToEvents(@QueryParam("url")
                              url: String? = null): Response =
            (if (url != null)
                monitor.fetchEventSubscriptionList(url)
            else
                monitor.fetchEventSubscriptionList())
                    .flatMap {
                        monitor.fetchEventSubscriptionList()
                    }
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_FETCH_SUBSCRIBED_TO_EVENTS) },
                            { ok(mapOf("events" to it)) }
                    ).build()

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("webhook/events")
    fun checkSubscribedToEvents(@QueryParam("url")
                                url: String? = null,
                                events: List<String>): Response =
            (if (url != null)
                monitor.checkEventSubscriptionList(url, events)
            else
                monitor.checkEventSubscriptionList(events))
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_SUBSCRIBED_TO_EVENTS) },
                            { ok(mapOf("match" to it)) }
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
                            { ok(mapOf("failedEvents" to it)) }
                    ).build()

    @GET
    @Path("events/fetch/failed")
    fun fetchEventsNotDeliverd(@QueryParam("interval")
                               interval: Long = 7200L): Response =
            monitor.fetchEvents(after = interval,
                    before = 0L,
                    state = StripeEventState.FAILED_TO_DELIVER)
                    .flatMap {
                        publishFailedEvents(it)
                    }
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_EVENTS_NOT_DELIVERED) },
                            { ok(mapOf("fetchedEvents" to it)) }
                    ).build()

    @GET
    @Path("stripe/transactions")
    fun fetchStripeTransactions(@QueryParam("after")
                                after: Long = epoch(-86400),
                                @QueryParam("before")
                                before: Long = 0L): Response =
            monitor.getTransactions(after, before)
                    .flatMap {
                        it.map {
                            mapOf(
                                    "id" to it.id,
                                    "status" to it.status,
                                    "currency" to it.currency,
                                    "amount" to it.amount,
                                    "invoiceId" to it.invoice
                            )
                        }.right()
                    }
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_FETCH_PAYMENT_TRANSACTIONS) },
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

    private fun ok(value: List<Any>) =
            Response.status(Response.Status.OK).entity(asJson(value))

    private fun ok(value: Map<String, Any>) =
            Response.status(Response.Status.OK).entity(asJson(value))

    private fun failed(error: PaymentError, code: ApiErrorCode): Response.ResponseBuilder =
            mapPaymentErrorToApiError(description = error.description,
                    errorCode = code,
                    paymentError = error).let {
                Response.status(it.status).entity(asJson(it))
            }
}
