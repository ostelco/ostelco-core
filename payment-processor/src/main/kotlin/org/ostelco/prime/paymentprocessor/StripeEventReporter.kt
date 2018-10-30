package org.ostelco.prime.paymentprocessor

import com.stripe.model.*
import org.ostelco.prime.getLogger
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER

class StripeEventReporter {

    private val logger by getLogger()

    fun handleEvent(event: Event) {

        val data = event.data.`object`

        when (data) {
            is Balance -> report(event, data)
            is Card -> report(event, data)
            is Charge -> report(event, data)
            is Customer -> report(event, data)
            is Dispute -> report(event, data)
            is Payout -> report(event, data)
            is Plan -> report(event, data)
            is Product -> report(event, data)
            is Refund -> report(event, data)
            is Source -> report(event, data)
            is Subscription -> report(event, data)
            else -> {
                logger.error(NOTIFY_OPS_MARKER, "No handler found for Stripe event ${event.type}")
            }
        }
    }

    private fun report(event: Event, balance: Balance) {
        when {
            event.type == "balance.available" -> logger.info(NOTIFY_OPS_MARKER,
                    "Your balance has new available transactions" +
                            "${currency(balance.available[0].amount, balance.available[0].currency)} is available, " +
                            "${currency(balance.pending[0].amount, balance.pending[0].currency)} is pending." +
                            url(event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    "Unhandled Stripe event ${event.type} (cat: Balance) " +
                            url(event))
        }
    }

    private fun report(event: Event, card: Card) {
        when {
            event.type == "customer.source.created" -> logger.info(NOTIFY_OPS_MARKER,
                    "${email(card.customer)} added a new ${card.brand} ending in ${card.last4} " +
                            url(event)
            )
            event.type == "customer.source.deleted" -> logger.info(NOTIFY_OPS_MARKER,
                    "${email(card.customer)} deleted a ${card.brand} ending in ${card.last4} " +
                            url(event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    "Unhandled Stripe event ${event.type} (cat: Card) " +
                            url(event))
        }
    }

    private fun report(event: Event, charge: Charge) {
        when {
            event.type == "charge.captured" -> logger.info(NOTIFY_OPS_MARKER,
                    "${email(charge.customer)}'s payment was captured for ${currency(charge.amount, charge.currency)} " +
                            url(event)
            )
            event.type == "charge.succeeded" -> logger.info(NOTIFY_OPS_MARKER,
                    "An uncaptured payment for ${currency(charge.amount, charge.currency)} was created for ${email(charge.customer)} " +
                            url(event)
            )
            event.type == "charge.refunded" -> logger.info(NOTIFY_OPS_MARKER,
                    "A ${currency(charge.amount, charge.currency)} payment was refunded to ${email(charge.customer)} " +
                            url(event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    "Unhandled Stripe event ${event.type} (cat: Charge) " +
                            url(event))
        }
    }

    private fun report(event: Event, customer: Customer) {
        when {
            event.type == "customer.created" -> logger.info(NOTIFY_OPS_MARKER,
                    "${customer.email} is a new customer " +
                            url(event)
            )
            event.type == "customer.deleted" -> logger.info(NOTIFY_OPS_MARKER,
                    "${customer.email} had been deleted " +
                            url(event)
            )
            event.type == "customer.updated" -> logger.info(NOTIFY_OPS_MARKER,
                    "${customer.email}'s details where updated " +
                            url(event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    "Unhandled Stripe event ${event.type} (cat: Customer) " +
                            url(event))
        }
    }

    private fun report(event: Event, dispute: Dispute) {
        logger.error(NOTIFY_OPS_MARKER,
                "Unhandled Stripe event ${event.type} (cat: Dispute) " +
                        url(event))
    }

    private fun report(event: Event, payout: Payout) {
        when {
            event.type == "payout.created" -> logger.info(NOTIFY_OPS_MARKER,
                    "A new payout for ${currency(payout.amount, payout.currency)} was created and will be deposited " +
                            "on ${epochToDate(payout.arrivalDate)}" +
                            url(event)
            )
            event.type == "payout.paid" -> logger.info(NOTIFY_OPS_MARKER,
                    "A payout of ${currency(payout.amount, payout.currency)} should now appear on your bank account statement " +
                            url(event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    "Unhandled Stripe event ${event.type} (cat: Payout) " +
                            url(event))
        }
    }

    private fun report(event: Event, plan: Plan) {
        logger.error(NOTIFY_OPS_MARKER,
                "Unhandled Stripe event ${event.type} (cat: Plan) " +
                        url(event))
    }

    private fun report(event: Event, product: Product) {
        logger.error(NOTIFY_OPS_MARKER,
                "Unhandled Stripe event ${event.type} (cat: Product) " +
                        url(event))
    }

    private fun report(event: Event, refund: Refund) {
        logger.error(NOTIFY_OPS_MARKER,
                "Unhandled Stripe event ${event.type} (cat: Refund) " +
                        url(event))
    }

    private fun report(event: Event, source: Source) {
        when {
            event.type == "customer.source.created" -> logger.info(NOTIFY_OPS_MARKER,
                    "${email(source.customer)} added a new payment source " +
                            url(event)
            )
            event.type == "customer.source.deleted" -> logger.info(NOTIFY_OPS_MARKER,
                    "Customer ${email(source.customer)} deleted a payment source " +
                            url(event)
            )
            event.type == "source.chargeable" -> logger.info(NOTIFY_OPS_MARKER,
                    "A source with ID ${source.id} is chargeable " +
                            url(event)
            )
            else -> logger.error(NOTIFY_OPS_MARKER,
                    "Unhandled Stripe event ${event.type} (cat: Source)" +
                            url(event))
        }
    }

    private fun report(event: Event, subscription: Subscription) {
        logger.error("Unhandled Stripe event ${event.type} (cat: Subscription) " +
                url(event))
    }

    private fun url(event: Event): String {
        return "https://dashboard.stripe.com/events/${event.id}"
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

    // TODO: Fix this!
    private fun epochToDate(ts: Long): String {
        return "YYYY/MM/DD"
    }
}