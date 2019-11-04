package org.ostelco.prime.paymentprocessor.subscribers

import arrow.core.Try
import arrow.core.getOrElse
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.stripe.model.*
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.ConfigRegistry
import org.ostelco.prime.paymentprocessor.core.PaymentStatus
import org.ostelco.prime.paymentprocessor.core.SubscriptionPaymentInfo
import org.ostelco.prime.pubsub.PubSubSubscriber
import org.ostelco.prime.storage.AdminDataSource
import java.time.Instant

/**
 * Acts on Stripe events related to renewal of subscriptions.
 *
 * Error cases are handled according to the recommendations at:
 * https://stripe.com/docs/billing/subscriptions/payment#building-your-own-handling-for-recurring-charge-failures
 */
class RecurringPaymentStripeEvent : PubSubSubscriber(
        subscription = ConfigRegistry.config.stripeEventRecurringPaymentSubscriptionId,
        topic = ConfigRegistry.config.stripeEventTopicId,
        project = ConfigRegistry.config.projectId) {

    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    private val gson = Gson()

    override fun handler(message: ByteString, consumer: AckReplyConsumer) =
            Try {
                gson.fromJson(message.toStringUtf8(), Event::class.java)
            }.fold(
                    ifSuccess = { event ->
                        Try {
                            val eventType = event.type
                            val data = event.data.`object`  /* See comment in 'Reporter.kt'. */

                            /* Only invoices are of interrest vs. recurring payment (I think). */
                            when (data) {
                                is Invoice -> invoiceEvent(eventType, data)
                                is Subscription -> subscriptionEvent(eventType, data)
                            }
                        }.getOrElse {
                            logger.error("Processing Stripe event {} failed with message: {}",
                                    message.toStringUtf8(), it.message)
                        }
                        consumer.ack()
                    },
                    ifFailure = {
                        logger.error("Failed to decode JSON Stripe event for 'recurring payment' processing: {}",
                                it.message)
                        consumer.ack()
                    }
            )

    /* Upcoming renewal and successfully renewed subscriptions. */
    private fun invoiceEvent(eventType: String, invoice: Invoice) {
        if (invoice.subscription.isNullOrEmpty())
            return
        if (invoice.billingReason != "subscription_cycle")
            return

        when (eventType) {
            /* Early notification (configurable in Stripe console) of renewal. */
            "invoice.upcoming" -> {
                invoiceUpcoming(invoice)
            }
            /* Sent right before subscription renewal. */
            "invoice.created" -> {
                invoiceCreated(invoice)
            }
            /* Subscription successfully renewed. */
            "invoice.payment_succeeded" -> {
                invoicePaymentSucceeded(invoice)
            }
        }
    }

    /* Failed payment on renewal. */
    private fun subscriptionEvent(eventType: String, subscription: Subscription) {
        if (eventType != "customer.subscription.updated")
            return
        if (subscription.status != "past_due")
            return

        val invoice = Invoice.retrieve(subscription.latestInvoice)
        val intent = PaymentIntent.retrieve(invoice.paymentIntent)

        when (intent.status) {
            /* No funds. */
            "requires_payment_method" -> {
                requiresPaymentMethod(subscription, invoice)
            }
            /* 3D secure. */
            "requires_action" -> {
                requiresUserAction(subscription, invoice)
            }
        }
    }

    private fun invoiceUpcoming(invoice: Invoice) {
        val subscription = Subscription.retrieve(invoice.subscription)
        val plan = subscription.plan

        storage.notifySubscriptionToPlanRenewalUpcoming(
                customerId = invoice.customer,
                sku = plan.product,
                dueDate = ofEpochSecondToMilli(invoice.dueDate)
        ).mapLeft {
            logger.error("Notification to customer ${invoice.customer} of upcoming renewal of subscription ${subscription.id} " +
                    "to plan ${plan.product} failed with error message ${it.message}")
        }
    }

    private fun invoiceCreated(invoice: Invoice) {
        val subscription = Subscription.retrieve(invoice.subscription)
        val plan = subscription.plan

        /* Force immediate payment in acceptance tests.
           If false then Stripe will do a charge attempt after 1 hour. */
        val payInvoiceNow = System.getenv("ACCEPTANCE_TESTING") == "true"

        storage.notifySubscriptionToPlanRenewalStarting(
                customerId = invoice.customer,
                sku = plan.product,
                invoiceId = invoice.id,
                payInvoiceNow = payInvoiceNow
        ).mapLeft {
            logger.error("Notification to customer ${invoice.customer} of subscription ${subscription.id} " +
                    "to plan ${plan.product} beiing renewed failed with error message ${it.message}")
        }
    }

    private fun invoicePaymentSucceeded(invoice: Invoice) {
        if (invoice.status != "paid") {
            logger.debug("Invoice ${invoice.id} for customer ${invoice.customer} paid with status ${invoice.status}")
            return
        }
        val subscription = Subscription.retrieve(invoice.subscription)
        val plan = subscription.plan

        storage.renewedSubscriptionToPlanSuccessfully(
                customerId = invoice.customer,
                sku = plan.product,
                subscriptionPaymentInfo = buildSubscriptionPaymentInfo(
                        status = PaymentStatus.PAYMENT_SUCCEEDED,
                        subscription = subscription,
                        plan = plan,
                        invoice = invoice
                )
        ).mapLeft {
            logger.error("Storing subscription renewal information for plan ${plan.product} for customer ${invoice.customer} " +
                    "and invoice ${invoice.id} failed with error message: ${it.message}")
        }
    }

    private fun requiresPaymentMethod(subscription: Subscription, invoice: Invoice) {
        val plan = subscription.plan

        storage.subscriptionToPlanRenewalFailed(
                customerId = subscription.customer,
                sku = plan.product,
                subscriptionPaymentInfo = buildSubscriptionPaymentInfo(
                        status = PaymentStatus.REQUIRES_PAYMENT_METHOD,
                        subscription = subscription,
                        plan = plan,
                        invoice = invoice
                )
        ).mapLeft {
            logger.error("Notification of subscriber ${invoice.customer} of failed payment for subscription ${plan.product}" +
                    "with invoice ${invoice.id} failed with error message: ${it.message}")
        }
    }

    /* TODO: (kmm) Add support.*/
    private fun requiresUserAction(subscription: Subscription, invoice: Invoice) {
        logger.error("Unexpected request for 'user action' on renewal of subscription ${subscription.id}")
    }

    private fun buildSubscriptionPaymentInfo(status: PaymentStatus,
                                             subscription: Subscription,
                                             plan: Plan,
                                             invoice: Invoice) =
            SubscriptionPaymentInfo(
                    id = subscription.id,
                    sku = plan.product,
                    status = status,
                    invoiceId = invoice.id,
                    chargeId = invoice.charge,
                    created = ofEpochSecondToMilli(subscription.created),
                    currentPeriodStart = ofEpochSecondToMilli(subscription.currentPeriodStart),
                    currentPeriodEnd = ofEpochSecondToMilli(subscription.currentPeriodEnd),
                    trialEnd = ofEpochSecondToMilli(subscription.trialEnd ?: 0L)
            )

    /* Timestamps in Prime must be in milliseconds. */
    private fun ofEpochSecondToMilli(ts: Long): Long = Instant.ofEpochSecond(ts).toEpochMilli()
}
