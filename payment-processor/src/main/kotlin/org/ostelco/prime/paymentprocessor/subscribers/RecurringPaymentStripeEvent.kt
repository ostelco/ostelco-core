package org.ostelco.prime.paymentprocessor.subscribers

import arrow.core.Try
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.protobuf.ByteString
import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.Invoice
import com.stripe.model.Product
import com.stripe.net.ApiResource
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.ConfigRegistry
import org.ostelco.prime.pubsub.PubSubSubscriber
import org.ostelco.prime.storage.AdminDataSource

class RecurringPaymentStripeEvent : PubSubSubscriber(
        subscription = ConfigRegistry.config.stripeEventRecurringPaymentSubscriptionId,
        topic = ConfigRegistry.config.stripeEventTopicId,
        project = ConfigRegistry.config.projectId) {

    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    override fun handler(message: ByteString, consumer: AckReplyConsumer) =
            Try {
                ApiResource.GSON.fromJson(message.toStringUtf8(), Event::class.java)
            }.fold(
                    ifSuccess = { event ->
                        val data = event.data.`object`

                        // FIXME Add proper handlig of error cases
                        consumer.ack()

                        /* Only invoices are of interrest vs. recurring payment (I think). */
                        when (data) {
                            is Invoice -> report(event, data)
                        }
                    },
                    ifFailure = {
                        logger.error("Failed to decode JSON Stripe event for 'recurring payment' processing: {}",
                                it)
                        /* If unparsable JSON then this should not affect
                           upstream, as the message is invalid. */
                        consumer.ack()
                    }
            )

    private fun report(event: Event, invoice: Invoice) {

        if (event.type == "invoice.payment_succeeded" && !invoice.subscription.isNullOrEmpty()) {

            val plan = invoice.lines.data[0].plan
            val productId = plan.product
            val amount = plan.amount
            val currency = plan.currency
            val productDetails = Product.retrieve(productId)
            val customer = Customer.retrieve(invoice.customer)

            // Not checking the return value for now (kmm)
            storage.subscriptionPurchaseReport(invoice.id, customer.email, productDetails.name, amount, currency)
        }
    }
}