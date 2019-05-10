package org.ostelco.prime.paymentprocessor.subscribers

import arrow.core.Try
import arrow.core.getOrElse
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.stripe.model.*
import com.stripe.net.ApiResource.GSON
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.ConfigRegistry
import org.ostelco.prime.pubsub.PubSubSubscriber
import org.ostelco.prime.storage.AdminDataSource
import org.ostelco.prime.storage.ValidationError

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
                            }
                        }.getOrElse {
                            logger.error("Attempt to log Stripe event {} failed with error message: {}",
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

    private fun invoiceEvent(eventType: String, invoice: Invoice) {
        /* Skip invoices not related to subscriptions (recurring payment). */
        if (invoice.subscription.isNullOrEmpty())
            return

        when (eventType) {
            "invoice.payment_succeeded" -> invoice.lines.data.forEach{
                purchasedSubscriptionEvent(invoice.id, invoice.customer, it.plan)
            }
            "invoice.payment_failed" -> {}
            "invoice.upcoming" -> {}
            "invoice.created" -> {}
            // on canceled subsc. F.ex. with expired payment
            "invoice.updated" -> {}
            "invoice.voided" -> {}
            "customer.subscription.updated" -> {}
        }
    }

    private fun purchasedSubscriptionEvent(invoiceId: String, customerId: String, plan: Plan) {
        val productId = plan.product
        val productDetails = Product.retrieve(productId)
        storage.purchasedSubscription(invoiceId, customerId, productDetails.name, plan.amount, plan.currency)
                .mapLeft {
                    when (it) {
                        is ValidationError -> {
                            /* Ignore as the purchase has already been registered (due to direct
                               charge being done when purchasing a subscription). */
                        }
                        else -> {
                            logger.error("Adding subscription purchase report for invoice {} failed with error message: {}",
                                    invoiceId, it.message)
                        }
                    }
                }
    }
}
