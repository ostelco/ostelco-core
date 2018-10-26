package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import com.stripe.model.Event
import org.ostelco.prime.getLogger
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER

class StripeEventReporter {

    private val logger by getLogger()

    fun reportEvent(event: Event): Either<String, String> {

        logger.info(NOTIFY_OPS_MARKER, "Got Stripe event ${event.type}}")

        return Either.right("ok")
    }

    /* Ripped from https://github.com/stripe/stripe-webhook-monitor/blob/master/public/app.js */
/*
    private fun summary(event: Event): String {

        return when {
            event.type == "account.external_account.created" -> "A new external account was created."
            event.type == "account.external_account.deleted" -> "A new external account was deleted."
            event.type == "account.external_account.updated" -> "A new external account was updated."
            event.type == "account.updated" -> "The Stripe account was updated."
            event.type == "balance.available" -> "The balance for this Stripe account was updated: "  +
                    "${currency(event.available[0].amount / 100, event.available.currency)} is available, " +
                    "${currency(event.pending[0].amount / 100, event.pending.currency)} is pending."
            event.type == "charge.captured" -> "Customer ${url('customers/' + event.customer, event.customer)}s\'" +
                    "charge for ${currency(event.amount / 100, event.currency)} was " +
                    "${url('charges/' + event.id, 'captured')}."
            event.type == "charge.dispute.closed" -> "The ${url('disputes/' + event.id, 'dispute')} for a " +
                    "${url('charges/' + event.charge, 'charge')} was closed."
            event.type == "charge.dispute.created" -> "The ${url('disputes/' + event.id, 'dispute')} for a " +
                    "${url('charges/' + event.charge, 'charge')} was created."
            event.type == "charge.dispute.funds_reinstated" -> "${currency(event.amount / 100, event.currency)} " +
                    "was reinstated to the Stripe account following a ${url('disputes/' + event.id, 'dispute')}."
            event.type == "charge.dispute.funds_withdrawn" -> "${currency(event.amount / 100, event.currency)} was " +
                    "withdrawn from the Stripe account following a ${url('disputes/ + event.id, 'dispute)}."
            event.type == "charge.dispute.updated" -> "The ${url('disputes/' + event.id, 'dispute')} for a
                ${url('charges/' + event.charge, 'charge')} was updated."
                event.type == "charge.failed" -> "A recent ${url('charges/' + event.id, 'charge')} for " +
                "${currency(event.amount / 100, event.currency)} failed."
            event.type == "charge.pending" -> "A recent ${url('charges/' + event.id, 'charge')} for " +
                    "${currency(event.amount / 100, event.currency)} is pending."
            event.type == "charge.refund.updated" -> "A ${currency(event.amount / 100, event.currency)} refund for a " +
                    "${url('charges/' + event.id, 'charge')} was updated."
            event.type == "charge.refunded" -> "A ${currency(event.amount / 100, event.currency)} " +
                    "${url('charges/' + event.id, 'charge')} was refunded."
            event.type == "charge.succeeded" -> "A ${url('customers/' + event.customer, 'customer')} " +
                    "was charged ${currency(event.amount / 100, event.currency)} " +
                    "with a ${event.source.brand} ${event.source.funding} ${event.source.object}."
            event.type == "charge.updated" -> "A ${currency(event.amount / 100, event.currency)} " +
                    "${url('charges/' + event.id, 'charge')} was updated."
            event.type == "coupon.created" -> "A coupon was created."
            event.type == "coupon.deleted" -> "A coupon was deleted."
            event.type == "coupon.updated" -> "A coupon was updated."
            event.type == "customer.bank_account.deleted" -> "A ${url('customers/' + event.id, 'customer')}\'s bank account was deleted."
            event.type == "customer.created" -> "A ${url('customers/' + event.id, 'new customer')} " +
                    "${event.email ? '(' + event.email + ')' : ''} was created."
            event.type == "customer.deleted" -> "A ${url('customers/' + event.id, ' customer')} " +
                    "${event.email ? '(' + event.email + ')' : ''} was deleted."
            event.type == "customer.discount.created" -> "A discount for a ${url('customers/' + event.id, 'customer')} was created."
            event.type == "customer.discount.deleted" -> "A discount for a ${url('customers/' + event.id, 'customer')} was deleted."
            event.type == "customer.discount.updated" -> "A discount for a ${url('customers/' + event.id, 'customer')} was updated."
            event.type == "customer.source.created" -> "A ${url('customers/' + event.customer, 'customer')} added a new payment source."
            event.type == "customer.source.deleted" -> "A ${url('customers/' + event.customer, 'customer')} deleted a payment source."
            event.type == "customer.source.updated" -> "A ${url('customers/' + event.customer, 'customer')} updated a payment source."
            event.type == "customer.subscription.created" -> "A ${url('customers/' + event.customer, 'customer')} " +
                    "created a new ${url('subscriptions/' + event.id, 'subscription')} to the " +
                    "${url('plans/' + event.plan.id, event.plan.name)} plan."
            event.type == "customer.subscription.deleted" -> "A ${url('customers/' + event.customer, 'customer')} " +
                    "deleted a ${url('subscriptions/' + event.id, 'subscription')} to the " +
                    "${url('plans/' + event.plan.id, event.plan.name)} plan."
            event.type == "customer.subscription.trial_will_end" -> "A ${url('customers/' + event.customer, 'customer')}\'s trial " +
                    "${url('subscriptions/' + event.id, 'subscription')} will end on ${date(event.trial_end)}."
            event.type == "customer.subscription.updated" -> "A ${url('customers/' + event.customer, 'customer')}\'s " +
                    "${url('subscriptions/' + event.id, 'subscription')} was updated."
            event.type == "customer.updated" -> "A ${url('customers/' + event.customer, 'customer')} was updated."
            event.type == "file.created" -> "A new file was uploded."
            event.type == "invoice.created" -> "A ${url('customers/' + event.customer, 'customer')}\'s " +
                    "${url('invoices/' + event.id, 'invoice')} was created."
            event.type == "invoice.payment_failed" -> "A ${url('customers/' + event.customer, 'customer')}\'s " +
                    "${url('invoices/' + event.id, 'invoice')} invoice payment failed."
            event.type == "invoice.payment_succeeded" -> "A ${url('customers/' + event.customer, 'customer')}\'s " +
                    "${url('invoices/' + event.id, 'invoice')} was successfully charged."
            event.type == "invoice.sent" -> "A ${url('customers/' + event.customer, 'customer')}\'s " +
                    "${url('invoices/' + event.id, 'invoice')} was sent."
            event.type == "invoice.upcoming" -> "A ${url('customers/' + event.customer, 'customer')}\'s " +
                    "${url('invoices/' + event.id, 'invoice')} was updated."
            event.type == "invoice.updated" -> "A ${url('customers/' + event.customer, 'customer')}\'s " +
                    "${url('invoices/' + event.id, 'invoice')} was updated."
            event.type == "invoiceitem.created" -> "A ${url('customers/' + event.customer, 'customer')} created an invoice " +
                    "item${event.invoice ? ' for an ' + url('invoices/' + event.invoice, 'invoice') : ''}."
            event.type == "invoiceitem.deleted" -> "A ${url('customers/' + event.customer, 'customer' )} deleted an invoice " +
                    "item${event.invoice ? ' for an ' + url('invoices/' + event.invoice, 'invoice') : ''}."
            event.type == "invoiceitem.updated" -> "A ${url('customers/' + event.customer, 'customer')} updated an invoice " +
                    "item${event.invoice ? ' for an ' + url('invoices/' + event.invoice, 'invoice') : ''}."
            event.type == "order.created" -> "A new ${url('orders/' + event.id, 'order')} for " +
                    "${currency(event.amount / 100)} was created."
            event.type == "order.payment_failed" -> "A payment failed for an ${url('orders/' + event.id, 'order')} for " +
                    "${currency(event.amount / 100)}."
            event.type == "order.payment_succeeded" -> "A payment succeeded for an ${url('orders/' + event.id, 'order')} for " +
                    "${currency(event.amount / 100)}."
            event.type == "order.updated" -> "An ${url('orders/' + event.id, 'order')} for ${currency(event.amount / 100)} was updated."
            event.type == "order_return.created" -> "A return was created for an ${url('orders/' + event.order, 'order')} " +
                    "for ${currency(event.amount / 100)}."
            event.type == "payout.canceled" -> "A payout of ${currency(event.amount / 100)} was canceled."
            event.type == "payout.created" -> "A payout of ${currency(event.amount / 100)} was initiated."
            event.type == "payout.failed" -> "A payout of ${currency(event.amount / 100)} was failed."
            event.type == "payout.paid" -> "A payout of ${currency(event.amount / 100)} was paid."
            event.type == "payout.updated" -> "A payout of ${currency(event.amount / 100)} was updated."
            event.type == "plan.created" -> "Plan ${url('plans/' + event.id, event.name)} was created."
            event.type == "plan.deleted" -> "Plan ${event.name} was deleted."
            event.type == "plan.updated" -> "Plan ${event.name} was updated."
            event.type == "product.created" -> "A new ${url('products/' + event.id, 'product')} was created."
            event.type == "product.deleted" -> "A ${url('products/' + event.id, 'product')} was deleted."
            event.type == "product.updated" -> "A ${url('products/' + event.id, 'product')} was updated."
            event.type == "recipient.created" -> "A new recipient was created."
            event.type == "recipient.deleted" -> "A new recipient was deleted."
            event.type == "recipient.updated" -> "A new recipient was updated."
            event.type == "review.closed" -> "A fraud review was closed."
            event.type == "review.opened" -> "A fraud review was opened."
            event.type == "sku.created" -> "A ${url('products/' + event.product, 'product')} SKU was created."
            event.type == "sku.deleted" -> "A ${url('products/' + event.product, 'product')} SKU was deleted."
            event.type == "sku.updated" -> "A ${url('products/' + event.product, 'product')} SKU was updated."
            event.type == "source.canceled" -> "A payment source was canceled."
            event.type == "source.chargeable" -> "A payment source is now chargeable."
            event.type == "source.failed" -> "A payment source failed."
            event.type == "source.transaction_created" -> "A transaction was created for a payment source."
            event.type == "transfer.created" -> "A transfer was created."
            event.type == "transfer.reversed" -> "A transfer was reversed."
            event.type == "transfer.updated" -> "A transfer was updated."
            else -> ""
        }
    }
*/
}