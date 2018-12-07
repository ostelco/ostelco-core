package org.ostelco.prime.paymentprocessor

import com.stripe.model.*
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.storage.AdminDataSource
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class StripeEventReporter {

    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    fun handleEvent(event: Event) {

        val data = event.data.`object`

        when (data) {
            is Balance -> report(event, data)
            is Card -> report(event, data)
            is Charge -> report(event, data)
            is Customer -> report(event, data)
            is Dispute -> report(event, data)
            is Invoice -> report(event, data)
            is Payout -> report(event, data)
            is Plan -> report(event, data)
            is Product -> report(event, data)
            is Refund -> report(event, data)
            is Source -> report(event, data)
            is Subscription -> report(event, data)
            else -> {
                logger.error(NOTIFY_OPS_MARKER, format("No handler found for Stripe event ${event.type}",
                        event))
            }
        }
    }

    private fun report(event: Event, balance: Balance) {
        when {
            event.type == "balance.available" -> logger.info(NOTIFY_OPS_MARKER,
                    format("Your balance has new available transactions" +
                            "${currency(balance.available[0].amount, balance.available[0].currency)} is available, " +
                            "${currency(balance.pending[0].amount, balance.pending[0].currency)} is pending.",
                            event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    format("Unhandled Stripe event ${event.type} (cat: Balance)",
                            event))
        }
    }

    private fun report(event: Event, card: Card) {
        when {
            event.type == "customer.source.created" -> logger.info(NOTIFY_OPS_MARKER,
                    format("${email(card.customer)} added a new ${card.brand} ending in ${card.last4}",
                            event)
            )
            event.type == "customer.source.deleted" -> logger.info(NOTIFY_OPS_MARKER,
                    format("${email(card.customer)} deleted a ${card.brand} ending in ${card.last4}",
                            event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    format("Unhandled Stripe event ${event.type} (cat: Card)",
                            event))
        }
    }

    private fun report(event: Event, charge: Charge) {
        when {
            event.type == "charge.captured" -> logger.info(NOTIFY_OPS_MARKER,
                    format("${email(charge.customer)}'s payment was captured for ${currency(charge.amount, charge.currency)}",
                            event)
            )
            event.type == "charge.succeeded" -> logger.info(NOTIFY_OPS_MARKER,
                    format("An uncaptured payment for ${currency(charge.amount, charge.currency)} was created for ${email(charge.customer)}",
                            event)
            )
            event.type == "charge.refunded" -> logger.info(NOTIFY_OPS_MARKER,
                    format("A ${currency(charge.amount, charge.currency)} payment was refunded to ${email(charge.customer)}",
                            event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    format("Unhandled Stripe event ${event.type} (cat: Charge)",
                            event))
        }
    }

    private fun report(event: Event, customer: Customer) {
        when {
            event.type == "customer.created" -> logger.info(NOTIFY_OPS_MARKER,
                    format("${customer.email} is a new customer",
                            event)
            )
            event.type == "customer.deleted" -> logger.info(NOTIFY_OPS_MARKER,
                    format("${customer.email} had been deleted",
                            event)
            )
            event.type == "customer.updated" -> logger.info(NOTIFY_OPS_MARKER,
                    format("${customer.email}'s details where updated",
                            event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    format("Unhandled Stripe event ${event.type} (cat: Customer)",
                            event))
        }
    }

    private fun report(event: Event, dispute: Dispute) {
        logger.error(NOTIFY_OPS_MARKER,
                format("Unhandled Stripe event ${event.type} (cat: Dispute)",
                        event))
    }

    private fun report(event: Event, payout: Payout) {
        when {
            event.type == "payout.created" -> logger.info(NOTIFY_OPS_MARKER,
                    format("A new payout for ${currency(payout.amount, payout.currency)} was created and will be deposited " +
                            "on ${millisToDate(payout.arrivalDate)}",
                            event)
            )
            event.type == "payout.paid" -> logger.info(NOTIFY_OPS_MARKER,
                    format("A payout of ${currency(payout.amount, payout.currency)} should now appear on your bank account statement",
                            event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    format("Unhandled Stripe event ${event.type} (cat: Payout)",
                            event))
        }
    }

    private fun report(event: Event, invoice: Invoice) {
        when {
            event.type == "invoice.payment_succeeded" -> {
                logger.info("invoice customer: ${invoice.customer}")
                logger.info("        subscription: ${invoice.subscription ?: "not a subscription"}")

                if (invoice.subscription != null) {
                    val plan = invoice.lines.data[0].plan
                    val productId = plan.product
                    val amount = plan.amount
                    val currency = plan.currency
                    val productDetails = Product.retrieve(productId)
                    val customer = Customer.retrieve(invoice.customer)
                    storage.subscriptionPurchaseReport(invoice.id, customer.email, productDetails.name, amount, currency)
                }
            }
            else -> logger.error(NOTIFY_OPS_MARKER,
                    format("Unhandled Stripe event ${event.type} (cat: Invoice)",
                            event))
        }
    }

    private fun report(event: Event, plan: Plan) {
        logger.error(NOTIFY_OPS_MARKER,
                format("Unhandled Stripe event ${event.type} (cat: Plan)",
                        event))
    }

    private fun report(event: Event, product: Product) {
        logger.error(NOTIFY_OPS_MARKER,
                format("Unhandled Stripe event ${event.type} (cat: Product)",
                        event))
    }

    private fun report(event: Event, refund: Refund) {
        logger.error(NOTIFY_OPS_MARKER,
                format("Unhandled Stripe event ${event.type} (cat: Refund)",
                        event))
    }

    private fun report(event: Event, source: Source) {
        when {
            event.type == "customer.source.created" -> logger.info(NOTIFY_OPS_MARKER,
                    format("${email(source.customer)} added a new payment source",
                            event)
            )
            event.type == "customer.source.deleted" -> logger.info(NOTIFY_OPS_MARKER,
                    format("Customer ${email(source.customer)} deleted a payment source",
                            event)
            )
            event.type == "source.chargeable" -> logger.info(NOTIFY_OPS_MARKER,
                    format("A source with ID ${source.id} is chargeable",
                            event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    "Unhandled Stripe event ${event.type} (cat: Source)" +
                            url(event.id))
        }
    }

    private fun report(event: Event, subscription: Subscription) {
        logger.error(format("Unhandled Stripe event ${event.type} (cat: Subscription)",
                event))
    }

    private fun format(s: String, event: Event): String {
        return "${s} ${url(event.id)}"
    }

    private fun url(eventId: String): String {
        return "https://dashboard.stripe.com/events/${eventId}"
    }

    private fun currency(amount: Long, currency: String): String {
        return when (currency.toUpperCase()) {
            "SGD", "USD" -> "\$"
            else -> ""
        } + "${amount / 100} ${currency.toUpperCase()}"
    }

    private fun email(customerId: String): String {
        val email = Customer.retrieve(customerId).email
        return if (email.isNullOrEmpty()) "****" else email
    }

    /* Convert millis since epoch to the date in YYYY-MM-DD format.
       Assumes millis to be UTC. */
    private fun millisToDate(ts: Long): String {
        val utc = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts),
                ZoneOffset.UTC)
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(utc)
    }
}