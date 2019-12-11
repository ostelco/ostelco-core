package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.stripe.Stripe
import com.stripe.model.Event
import com.stripe.model.WebhookEndpoint
import org.ostelco.prime.getLogger
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.paymentprocessor.StripeUtils.either
import org.ostelco.prime.paymentprocessor.core.NotFoundError
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

    private val logger by getLogger()

    /**
     * Log that the monitor is running. (Will be the only output in logs from
     * the monitor if nothing unexpected are found.)
     */
    fun hello() = logger.info("Periodic monitoring of Stripe executing")

    /**
     * Check for mismatch between Stripe API version used for events
     * and version used by the Java library.
     * API version mismatch might indiacate incomplete update to
     * a newer API verison. F.ex. that the library has been updated
     * to a newer API version, but not the API version used for
     * reporting events (configured using the Stripe console).
     * @return true on match
     */
    fun checkApiVersion(): Either<PaymentError, Boolean> =
            fetchLastEvent()
                    .flatMap {
                        if (it.size > 0)
                            when {
                                it.first().apiVersion == Stripe.API_VERSION -> true
                                else -> {
                                    logger.error(NOTIFY_OPS_MARKER,
                                            "Stripe API version mismatch between Stripe events and library, " +
                                                    "got ${it.first().apiVersion}, expected ${Stripe.API_VERSION}")
                                    false
                                }
                            }.right()
                        else {
                            logger.error("Failed to check API version as no Stripe event was found for Stripe account " +
                                    "(the API version is retrived from an event message)")
                            PaymentConfigurationError("No events found for Stripe account")
                                    .left()
                        }
                    }

    /**
     * Fetch list of subscribed to events from the first enabled webhook.
     * @return list of subscribed to events
     */
    fun fetchEventSubscriptionList(): Either<PaymentError, List<String>> =
            fetchWebhookEndpoints(1)
                    .flatMap {
                        if (it.size > 0)
                            it.first().enabledEvents.toList().right()
                        else
                            emptyList<String>().right()
                    }

    /**
     * Fetch list of subscribed to events from the webhook configured
     * with the 'url' endpoint.
     * @param url - URL of webhook
     * @return list of subscribed to events
     */
    fun fetchEventSubscriptionList(url: String): Either<PaymentError, List<String>> =
            fetchWebhookEndpoint(url)
                    .flatMap {
                            it.enabledEvents.toList().right()
                    }

    /**
     * Check if list of Stripe event subscriptions matches expected
     * subscriptions.
     * @param events - events names to check for
     * @return true if match
     */
    fun checkEventSubscriptionList(events: List<String>): Either<PaymentError, Boolean> =
            fetchWebhookEndpoints(1)
                    .flatMap {
                        when {
                            it.size > 0 &&
                                    it.first().enabledEvents.containsAll(events) &&
                                    it.first().enabledEvents.size == events.size -> true
                            else -> {
                                logger.error(NOTIFY_OPS_MARKER,
                                        "Stripe events subscription does not match expected list of events, diff: " +
                                                it.first().enabledEvents.plus(events)
                                                        .groupBy {
                                                            it
                                                        }.filter {
                                                            it.value.size == 1
                                                        }.map {
                                                            it.key
                                                        }.joinToString(limit = 5)
                                )
                                false
                            }
                        }.right()
                    }

    /**
     * Check if list of Stripe event subscriptions matches expected
     * subscriptions.
     * @param url - URL of a webhook
     * @param events - events names to check for
     * @return true if match
     */
    fun checkEventSubscriptionList(url: String, events: List<String>): Either<PaymentError, Boolean> =
            fetchWebhookEndpoint(url)
                    .flatMap {
                        when {
                            it.enabledEvents.containsAll(events) &&
                                    it.enabledEvents.size == events.size -> true
                            else -> {
                                logger.error(NOTIFY_OPS_MARKER,
                                        "Stripe events subscription for endpoint ${url} does not match expected list of events, diff: " +
                                                it.enabledEvents.plus(events)
                                                        .groupBy {
                                                            it
                                                        }.filter {
                                                            it.value.size == 1
                                                        }.map {
                                                            it.key
                                                        }.joinToString(limit = 5)
                                )
                                false
                            }
                        }.right()
                    }

    /**
     * Fetch last Stripe event.
     * @param state - all, failed to deliver of successfully delivered events
     * @return event
     */
    fun fetchLastEvent(state: StripeEventState = StripeEventState.ALL): Either<PaymentError, List<Event>> =
            fetchLastEvents(1, state)

    /**
     * Fetch one or more of the last sent Stripe events.
     * @param limit - max number of events to return
     * @param state - all, failed to deliver of successfully delivered events
     * @return list with event
     */
    fun fetchLastEvents(limit: Int = 1, state: StripeEventState = StripeEventState.ALL): Either<PaymentError, List<Event>> =
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
     * @param start - events sent on or later
     * @param end - events sent on or before
     * @param state - all, failed to deliver of successfully delivered events
     * @return list with event
     */
    fun fetchEventsWithinRange(start: Long = 0L, end: Long = 0L, state: StripeEventState = StripeEventState.ALL): Either<PaymentError, List<Event>> =
            either("Failed to fetch Stripe events from time period ${start} to ${end}") {
                val param = mapOf(
                        "created[gte]" to ofEpochMilliToSecond(start),
                        "created[lte]" to ofEpochMilliToSecond(end),
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
            checkWebhookState(expected = true)

    /**
     * Check if at least one Stripe webhook is disabled.
     * @return true if disabled
     */
    fun checkWebhookDisabled(): Either<PaymentError, Boolean> =
            checkWebhookState(expected = false)

    private fun checkWebhookState(expected: Boolean): Either<PaymentError, Boolean> =
            fetchWebhookEndpoints(1)
                    .flatMap {
                        if (it.size > 0)
                            when {
                                it.first().status == "enabled" && expected -> true
                                it.first().status == "disabled" && !expected -> true
                                else -> {
                                    logger.error(NOTIFY_OPS_MARKER, if (expected)
                                        "Stripe events (webhook) is not enabled"
                                    else
                                        "Stripe events (webhook) is not disabled")
                                    false
                                }
                            }.right()
                        else {
                            logger.error("No webhooks found on check for Stripe events state")
                            NotFoundError("No webhooks found on check for Stripe events state")
                                    .left()
                        }
                    }

    /**
     * Check if given Stripe webhook is enabled.
     * @param url - URL of webhook
     * @return true if enabled
     */
    fun checkWebhookEnabled(url: String): Either<PaymentError, Boolean> =
            checkWebhookState(expected = true, url = url)

    /**
     * Check if given Stripe webhook is enabled.
     * @param url - URL of webhook
     * @return true if enabled
     */
    fun checkWebhookDisabled(url: String): Either<PaymentError, Boolean> =
            checkWebhookState(expected = false, url = url)

    private fun checkWebhookState(expected: Boolean, url: String): Either<PaymentError, Boolean> =
            fetchWebhookEndpoint(url)
                    .flatMap {
                        when {
                            it.status == "enabled" && expected -> true
                            it.status == "disabled" && !expected -> true
                            else -> {
                                logger.error(NOTIFY_OPS_MARKER, if (expected)
                                    "Stripe events (webhook) for endpoint ${url} is not enabled"
                                else
                                    "Stripe events (webhook) for endpoint ${url} is not disabled")
                                false
                            }
                        }.right()
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

    /* Timestamps in Stripe must be in seconds. */
    private fun ofEpochMilliToSecond(ts: Long): Long = ts.div(1000L)
}
