package org.ostelco.prime.paymentprocessor.subscribers

import com.stripe.model.*
import org.ostelco.prime.getLogger
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object Reporter {

    private val logger by getLogger()

    fun report(event: Event) {
        /* TODO: (kmm) The recommend way is:
                     val data = event.dataObjectDeserializer.`object`
                  but that don't work for some reason...
                  Ref.: https://github.com/stripe/stripe-java/wiki/Migration-guide-for-v8----version-upgrade */
        val data = event.data.`object`

        when (data) {
            is Balance -> report(event, data)
            is Card -> report(event, data)
            is Charge -> report(event, data)
            is Customer -> report(event, data)
            is Dispute -> report(event, data)
            is Invoice -> report(event, data)
            is InvoiceItem -> report(event, data)
            is PaymentIntent -> report(event, data)
            is PaymentMethod -> report(event, data)
            is Payout -> report(event, data)
            is Plan -> report(event, data)
            is Product -> report(event, data)
            is Refund -> report(event, data)
            is Source -> report(event, data)
            is Subscription -> report(event, data)
            else -> logger.warn(format("No handler found for Stripe event ${event.type}",
                    event))
        }
    }

    private fun report(event: Event, balance: Balance) =
            when {
                event.type == "balance.available" -> logger.info(NOTIFY_OPS_MARKER,
                        format("Your balance has new available transactions " +
                                "${currency(balance.available[0].amount, balance.available[0].currency)} is available, " +
                                "${currency(balance.pending[0].amount, balance.pending[0].currency)} is pending.",
                                event)
                )
                else -> logger.warn(
                        format("Unhandled Stripe event ${event.type} (cat: Balance)",
                                event))
            }

    private fun report(event: Event, card: Card) =
            when {
                event.type == "customer.source.created" -> logger.debug(
                        format("${email(card.customer)} added a new ${card.brand} ending in ${card.last4}",
                                event)
                )
                event.type == "customer.source.deleted" -> logger.debug(
                        format("${email(card.customer)} deleted a ${card.brand} ending in ${card.last4}",
                                event)
                )
                else -> logger.warn(
                        format("Unhandled Stripe event ${event.type} (cat: Card)",
                                event))
            }

    private fun report(event: Event, charge: Charge) =
            when {
                event.type == "charge.captured" -> logger.info(
                        format("${email(charge.customer)}'s payment was captured for ${currency(charge.amount, charge.currency)}",
                                event)
                )
                event.type == "charge.succeeded" -> logger.info(
                        format("${email(charge.customer)} was charged ${currency(charge.amount, charge.currency)}",
                                event)
                )
                event.type == "charge.refunded" -> logger.info(
                        format("A ${currency(charge.amount, charge.currency)} payment was refunded to ${email(charge.customer)}}",
                                event)
                )
                event.type == "charge.failed" -> logger.info(
                        format("A ${currency(charge.amount, charge.currency)} payment from ${email(charge.customer)} failed",
                        event)
                )
                else -> logger.warn(
                        format("Unhandled Stripe event ${event.type} (cat: Charge)",
                                event))
            }

    private fun report(event: Event, customer: Customer) =
            when {
                event.type == "customer.created" -> logger.info(
                        format("${customer.email} is a new customer",
                                event)
                )
                event.type == "customer.deleted" -> logger.info(
                        format("${customer.email} had been deleted",
                                event)
                )
                event.type == "customer.updated" -> logger.debug(
                        format("${customer.email}'s details where updated",
                                event)
                )
                else -> logger.warn(
                        format("Unhandled Stripe event ${event.type} (cat: Customer)",
                                event))
            }

    private fun report(event: Event, dispute: Dispute) =
            logger.warn(
                    format("Unhandled Stripe event ${event.type} (cat: Dispute)",
                            event))

    private fun report(event: Event, payout: Payout) =
            when {
                event.type == "payout.created" -> logger.info(
                        format("A new payout for ${currency(payout.amount, payout.currency)} was created and will be deposited " +
                                "on ${millisToDate(payout.arrivalDate)}",
                                event)
                )
                event.type == "payout.paid" -> logger.info(
                        format("A payout of ${currency(payout.amount, payout.currency)} should now appear on your bank account statement",
                                event)
                )
                else -> logger.warn(
                        format("Unhandled Stripe event ${event.type} (cat: Payout)",
                                event))
            }

    private fun report(event: Event, invoice: Invoice) =
            when {
                event.type == "invoice.created" -> logger.debug(
                        format("A draft invoice was created}",
                                event)
                )
                event.type == "invoice.finalized" -> logger.debug(
                        format("A draft invoice for ${currency(invoice.amountDue, invoice.currency)} to ${invoice.customerEmail} was finalized",
                                event)
                )
                event.type == "invoice.updated" -> logger.debug(
                        format("${invoice.customerEmail}'s invoice has changed",
                                event)
                )
                event.type == "invoice.payment_succeeded" -> logger.info(
                        format("${invoice.customerEmail}'s invoice for ${currency(invoice.amountPaid, invoice.currency)} was paid",
                            event))
                event.type == "invoice.voided" -> logger.debug(
                        format("${invoice.customerEmail}'s invoice ${invoice.id} was voided",
                                event))
                event.type == "invoice.payment_failed" -> logger.info(NOTIFY_OPS_MARKER,
                        format("Payment of  ${invoice.customerEmail}'s invoice for ${currency(invoice.amountPaid, invoice.currency)} failed",
                                event)
                )
                else -> logger.warn(
                        format("Unhandled Stripe event ${event.type} (cat: Invoice)",
                                event))
            }

    private fun report(event: Event, item: InvoiceItem) =
            when {
                event.type == "invoiceitem.created" -> logger.debug(
                        format("An invoice item for ${currency(item.amount, item.currency)} was created for ${email(item.customer)}",
                            event)
                )
                event.type == "invoiceitem.updated" -> logger.debug(
                        format("${email(item.customer)}'s invoice item for ${currency(item.amount, item.currency)} was added to an invoice",
                                event)
                )
                else -> logger.warn(
                        format("Unhandled Stripe event ${event.type} (cat: InvoiceItem)",
                                event))
            }

    private fun report(event: Event, plan: Plan) =
            when {
                event.type == "plan.created" -> logger.info(
                        format("A new plan called ${plan.nickname} was crated",
                                event)
                )
                else -> logger.warn(
                        format("Unhandled Stripe event ${event.type} (cat: Plan)",
                                event))
            }

    private fun report(event: Event, product: Product) =
            when {
                event.type == "product.created" -> logger.info(
                        format("A product with ID ${product.id} was created",
                                event)
                )
                else -> logger.warn(
                        format("Unhandled Stripe event ${event.type} (cat: Product)",
                                event))
            }

    private fun report(event: Event, intent: PaymentIntent) =
            when {
                event.type == "payment_intent.created" -> logger.debug(
                        format("A new payment ${intent.id} for ${currency(intent.amount, intent.currency)} was created",
                                event)
                )
                event.type == "payment_intent.payment_failed" -> logger.warn(
                        format("Fauked to create payment ${intent.id} for ${currency(intent.amount, intent.currency)}",
                                event)
                )
                event.type == "payment_intent.succeeded" -> logger.debug(
                        format("The payment ${intent.id} for ${currency(intent.amount, intent.currency)} has succeeded",
                                event)
                )
                else -> logger.warn(
                        format("Unhandled Stripe event ${event.type} (cat: PaymentIntent)",
                                event))
            }

    private fun report(event: Event, method: PaymentMethod) =
            when {
                event.type == "payment_method.attached" -> {
                    if (method.type == "card")
                        logger.debug("A card payment method ending in ${method.card.last4} was attached to customer ${method.customer}")
                    else
                        /* TODO: (kmm) Add other payment methods. */
                        logger.warn(
                                format("Unhandled Stripe event ${event.type} (cat: PaymentMethod)",
                                        event))
                }
                event.type == "payment_method.detached" -> {
                    if (method.type == "card")
                        logger.debug("A card payment method ending in ${method.card.last4} was detached from customer ${method.customer}")
                    else
                    /* TODO: (kmm) Add other payment methods. */
                        logger.warn(
                                format("Unhandled Stripe event ${event.type} (cat: PaymentMethod)",
                                        event))
                }
                else -> logger.warn(
                        format("Unhandled Stripe event ${event.type} (cat: PaymentMethod)",
                                event))
            }

    private fun report(event: Event, refund: Refund) =
            logger.warn(
                    format("Unhandled Stripe event ${event.type} (cat: Refund)",
                            event))

    private fun report(event: Event, source: Source) =
            when {
                event.type == "customer.source.created" -> logger.debug(
                        format("${email(source.customer)} added a new payment source",
                                event)
                )
                event.type == "customer.source.deleted" -> logger.debug(
                        format("Customer ${email(source.customer)} deleted a payment source",
                                event)
                )
                event.type == "source.chargeable" -> logger.debug(
                        format("A source with ID ${source.id} is chargeable",
                                event)
                )
                else -> logger.warn(
                        "Unhandled Stripe event ${event.type} (cat: Source)" +
                                url(event.id))
            }

    private fun report(event: Event, subscription: Subscription) =
            when {
                event.type == "customer.subscription.created" -> logger.info(NOTIFY_OPS_MARKER,
                        format("${email(subscription.customer)} subscribed to ${subscription.plan.id}",
                                event)
                )
                event.type == "customer.subscription.updated" -> logger.info(
                        format("${email(subscription.customer)} subscription to ${subscription.plan.id} got updated",
                                event)
                )
                event.type == "customer.subscription.deleted" -> logger.info(
                        format("${email(subscription.customer)} subscription to ${subscription.plan.id} got deleted",
                                event)
                )
                else -> logger.warn(format("Unhandled Stripe event ${event.type} (cat: Subscription)",
                        event))
            }

    private fun format(s: String, event: Event): String = "${s} ${url(event.id)}"

    private fun url(eventId: String): String = "https://dashboard.stripe.com/events/${eventId}"

    private fun currency(amount: Long, currency: String): String =
            when (currency.toUpperCase()) {
                "SGD", "USD" -> "\$"
                else -> ""
            } + df.format(amount / 100.0) +
                    when (currency.toUpperCase()) {
                        "NOK" -> " kr"
                        else -> ""
                    }

    private fun email(customerId: String?): String {
        val email = if (!customerId.isNullOrEmpty())
            Customer.retrieve(customerId).email
        else
            null
        return if (email.isNullOrEmpty()) "****" else email
    }

    /* Convert millis since epoch to the date in YYYY-MM-DD format.
       Assumes millis to be UTC. */
    private fun millisToDate(ts: Long): String {
        val utc = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts),
                ZoneOffset.UTC)
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(utc)
    }

    /* Formatting of amounts. */
    val df = DecimalFormat("#,###.##")
}
