package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.stripe.Stripe
import com.stripe.model.Event
import com.stripe.model.WebhookEndpoint
import org.ostelco.prime.paymentprocessor.StripeUtils.either
import org.ostelco.prime.paymentprocessor.core.PaymentConfigurationError
import org.ostelco.prime.paymentprocessor.core.PaymentError

/* Delivery status requested when query for a Stripe event. */
enum class StripeEventState {
    ALL,
    FAILED_TO_DELIVER,
    DELIVERED
}

/**
 * Stripe monitoring.
 */
class StripeMonitor {

    /**
     * Check for mismatch between Stripe API version used for events
     * and version used by the Java library.
     * API version mismatch might indiacate incomplete update to
     * a newer API verison. F.ex. that the library has been updated
     * to a newer API version, but not the API version used for
     * reporting events (configured using the Stripe console).
     * @return true on match
     */
    fun checkVersion(): Either<PaymentError, Boolean> =
            fetchLastEvent()
                    .flatMap {
                        if (it.size > 0)
                            (it.first().apiVersion == Stripe.API_VERSION)
                                    .right()
                        else
                            PaymentConfigurationError("No events found for Stripe account")
                                    .left()
                    }

    /**
     * Check if list of Stripe event subscriptions matches expected
     * subscriptions.
     * @return true if match
     */
    fun checkEventSubscriptions(events: List<String>): Either<PaymentError, Boolean> =
            fetchWebhookEndpoints(1)
                    .flatMap {
                        (it.size > 0 &&
                                it.first().enabledEvents.containsAll(events) &&
                                it.first().enabledEvents.size == events.size)
                                .right()
                    }

    /**
     * Fetch last Stripe event.
     * @param state - all, failed to deliver of successfully delivered events
     * @return event
     */
    fun fetchLastEvent(state: StripeEventState = StripeEventState.ALL): Either<PaymentError, List<Event>> =
            fetchEvents(1, state)

    /**
     * Fetch one or more of the last sent Stripe events.
     * @param limit - max number of events to return
     * @param state - all, failed to deliver of successfully delivered events
     * @return list with event
     */
    fun fetchEvents(limit: Int = 1, state: StripeEventState = StripeEventState.ALL): Either<PaymentError, List<Event>> =
            either("Failed to fetch Stripe events") {
                val param = mapOf(
                        "limit" to if (limit < 10) limit else 10,
                        *(if (state == StripeEventState.FAILED_TO_DELIVER)
                            arrayOf("delivery_success" to false)
                        else if (state == StripeEventState.DELIVERED)
                            arrayOf("delivery_success" to true)
                        else arrayOf()))
                Event.list(param)
                        .autoPagingIterable()
                        .take(limit)
            }

    /**
     * Fetch Stripe events within the time range after..before, where the timestamps
     * are Epoch timestamps and the value 0 means ignore. Examples:
     *     1560124800..1560729599 - fetch all events from 2019-06-10 to 2019-06-16
     *     1560124800..0          - fetch all events from 2019-06-10 and up to today
     *     0..1560124800          - fetch all events from before 2019-06-10
     * @param after - events sent on or later
     * @param before - events sent on or before
     * @param state - all, failed to deliver of successfully delivered events
     * @return list with event
     */
    fun fetchEvents(after: Long = 0L, before: Long = 0L, state: StripeEventState = StripeEventState.ALL): Either<PaymentError, List<Event>> =
            either("Failed to fetch Stripe events from time period ${after} to ${before}") {
                val param = mapOf(
                        *(if (after > 0)
                            arrayOf("created[gte]" to after)
                        else arrayOf()),
                        *(if (before > 0)
                            arrayOf("created[lte]" to before)
                        else arrayOf()),
                        *(if (state == StripeEventState.FAILED_TO_DELIVER)
                            arrayOf("delivery_success" to false)
                        else if (state == StripeEventState.DELIVERED)
                            arrayOf("delivery_success" to true)
                        else arrayOf()))
                Event.list(param)
                        .autoPagingIterable()
                        .toList()
            }

    /**
     * Check if at least one Stripe webhook is enabled.
     * @return true if enabled
     */
    fun checkWebhookEnabled(): Either<PaymentError, Boolean> =
            fetchWebhookEndpoints(1)
                    .flatMap {
                        (it.size > 0 && it.first().status == "enabled")
                                .right()
                    }

    /**
     * Check if given Stripe webhook is enabled.
     * @param url - URL of webhook
     * @return true if enabled
     */
    fun checkWebhookEnabled(url: String): Either<PaymentError, Boolean> =
            fetchWebhookEndpoint(url)
                    .flatMap {
                        (it.status == "enabled").right()
                    }

    /**
     * Fetch given Stripe webhook details.
     * @param url - URL of webhook
     * @return true if enabled
     */
    fun fetchWebhookEndpoint(url: String): Either<PaymentError, WebhookEndpoint> =
            fetchAllWebhookEndpoints()
                    .flatMap {
                        val match = it.filter { it.url.startsWith(url) }
                                .firstOrNull()
                        if (match != null)
                            match.right()
                        else
                            PaymentConfigurationError("No webhook matching ${url}").left()
                    }

    /**
     * Fetch details aboout one or more Stripe Webhooks.
     * @param limit - number of webhooks to fetch
     * @return list with details
     */
    fun fetchWebhookEndpoints(limit: Int): Either<PaymentError, List<WebhookEndpoint>> =
            either("Failed to fetch configured Stripe webhooks details") {
                WebhookEndpoint.list(mapOf("limit" to if (limit < 10) limit else 10))
                        .autoPagingIterable()
                        .take(limit)
            }

    /**
     * Fetch details for all Stripe webhooks.
     * @return list with details
     */
    fun fetchAllWebhookEndpoints(): Either<PaymentError, List<WebhookEndpoint>> =
            either("Failed to fetch configured Stripe webhooks details") {
                WebhookEndpoint.list(emptyMap())
                        .autoPagingIterable()
                        .toList()
            }
}
