package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import org.ostelco.prime.paymentprocessor.core.InvoicePaymentInfo
import org.ostelco.prime.paymentprocessor.core.InvoiceInfo
import org.ostelco.prime.paymentprocessor.core.InvoiceItemInfo
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.PaymentTransactionInfo
import org.ostelco.prime.paymentprocessor.core.PlanInfo
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import org.ostelco.prime.paymentprocessor.core.SourceDetailsInfo
import org.ostelco.prime.paymentprocessor.core.SourceInfo
import org.ostelco.prime.paymentprocessor.core.SubscriptionPaymentInfo
import org.ostelco.prime.paymentprocessor.core.SubscriptionInfo
import org.ostelco.prime.paymentprocessor.core.TaxRateInfo
import java.math.BigDecimal

interface PaymentProcessor {

    enum class Interval(val value: String) {
        DAY("day"),
        WEEK("week"),
        MONTH("month"),
        YEAR("year")
    }

    /**
     * @param stripeCustomerId Stripe customer id
     * @param stripeSourceId Stripe source id
     * @return Stripe sourceId if created
     */
    fun addSource(stripeCustomerId: String, stripeSourceId: String): Either<PaymentError, SourceInfo>

    /**
     * Adds source and sets it as 'default source' if requested. If the source has
     * already been added it is not added again.
     * @param customerId Customer id
     * @param sourceId Stripe source id
     * @param setDefault If true then set the 'sourceId' as the default source
     * @return The source-info object describing the source
     */
    fun addSource(customerId: String,
                  sourceId: String,
                  setDefault: Boolean = false): Either<PaymentError, SourceInfo>

    /**
     * @param customerId: Prime unique identifier for customer
     * @param email: Contact email address
     * @return Stripe customerId if created
     */
    fun createPaymentProfile(customerId: String, email: String): Either<PaymentError, ProfileInfo>

    /**
     * @param stripeCustomerId Stripe customer id
     * @return Stripe customerId if deleted
     */
    fun deletePaymentProfile(stripeCustomerId: String): Either<PaymentError, ProfileInfo>

    /**
     * @param customerId: user email (Prime unique identifier for customer)
     * @return Stripe customerId if exist
     */
    fun getPaymentProfile(customerId: String): Either<PaymentError, ProfileInfo>

    /**
     * @param productId The product associated with the new plan
     * @param amount The amount to be charged in the interval specified
     * @param currency Three-letter ISO currency code in lowercase
     * @param interval The frequency with which a subscription should be billed
     * @param invervalCount The number of intervals between subscription billings
     * @return Stripe plan details
     */
    fun createPlan(productId: String, amount: Int, currency: String, interval: Interval, intervalCount: Long = 1): Either<PaymentError, PlanInfo>

    /**
     * @param Stripe Plan Id
     * @return Stripe PlanId if deleted
     */
    fun removePlan(planId: String): Either<PaymentError, PlanInfo>

    /**
     * Removes the product identified with 'productId' and all associated
     * price plans.
     * @param productId The plan product to be removed
     * @return Id of the removed plan product
     */
    fun removeProductAndPricePlans(productId: String): Either<PaymentError, ProductInfo>

    /**
     * @param Stripe Plan Id
     * @param stripeCustomerId Stripe Customer Id
     * @param trielEnd Epoch timestamp for when the trial period ends
     * @param taxRegion An identifier representing the taxes to be applied to a region
     * @return Stripe SubscriptionId if subscribed
     */
    fun createSubscription(planId: String, stripeCustomerId: String, trialEnd: Long = 0L, taxRegionId: String? = null): Either<PaymentError, SubscriptionPaymentInfo>

    /**
     * @param Stripe Subscription Id
     * @param Stripe invoiceNow set to true if a final invoice should be generated
     * @return Stripe SubscriptionId if unsubscribed
     */
    fun cancelSubscription(subscriptionId: String, invoiceNow: Boolean = false): Either<PaymentError, SubscriptionInfo>

    /**
     * @param productId Prime product Id
     * @param name Prime product name
     * @return Stripe productId if created
     */
    fun createProduct(productId: String, name: String): Either<PaymentError, ProductInfo>

    /**
     * @param productId Stripe product Id
     * @return Stripe productId if removed
     */
    fun removeProduct(productId: String): Either<PaymentError, ProductInfo>

    /**
     * @param stripeCustomerId Stripe customer id
     * @return List of Stripe sourceId
     */
    fun getSavedSources(stripeCustomerId: String): Either<PaymentError, List<SourceDetailsInfo>>

    /**
     * @param stripeCustomerId Stripe customer id
     * @return Stripe default sourceId
     */
    fun getDefaultSource(stripeCustomerId: String): Either<PaymentError, SourceInfo>

    /**
     * @param stripeCustomerId Stripe customer id
     * @param sourceId Stripe source id
     * @return SourceInfo if created
     */
    fun setDefaultSource(stripeCustomerId: String, sourceId: String): Either<PaymentError, SourceInfo>

    /**
     * @param customerId Customer id in the payment system
     * @param sourceId id of the payment source
     * @param amount The amount to be charged
     * @param currency Three-letter ISO currency code in lowercase
     * @return id of the charge if authorization was successful
     */
    fun authorizeCharge(customerId: String, sourceId: String?, amount: Int, currency: String): Either<PaymentError, String>

    /**
     * @param chargeId ID of the of the authorized charge from authorizeCharge()
     * @param customerId Customer id in the payment system
     * @return id of the charge if authorization was successful
     */
    fun captureCharge(chargeId: String, customerId: String, amount: Int, currency: String): Either<PaymentError, String>

    /**
     * @param chargeId ID of the of the authorized charge to refund from authorizeCharge()
     * @return id of the charge
     */
    fun refundCharge(chargeId: String): Either<PaymentError, String>

    /**
     * @param chargeId ID of the of the authorized charge to refund from authorizeCharge()
     * @param amount The amount to refund form the charge
     * @return id of the charge
     */
    fun refundCharge(chargeId: String, amount: Int): Either<PaymentError, String>

    /**
     * @param stripeCustomerId Customer id in the payment system
     * @param sourceId id of the payment source
     * @return id if removed
     */
    fun removeSource(stripeCustomerId: String, sourceId: String): Either<PaymentError, SourceInfo>

    fun getStripeEphemeralKey(customerId: String, email: String, apiVersion: String): Either<PaymentError, String>

    /**
     * @param customerId ID of the customer to which the invoice item will be assigned to
     * @param amount Amount to pay (an integer multiplied by 100)
     * @param currency The currency to use for the payment (ISO code)
     * @param description Short description of what the invoice item is for
     * @return ID of the invoice item
     */
    fun createInvoiceItem(customerId: String, amount: Int, currency: String, description: String): Either<PaymentError, InvoiceItemInfo>

    /**
     * @param invoiceItemId ID of invoice item to delete
     * @return ID of the deleted invoice item
     */
    fun removeInvoiceItem(invoiceItemId: String): Either<PaymentError, InvoiceItemInfo>

    /**
     * @param customerId ID of the customer to which the invoice will be assigned to
     * @param taxRates List of tax-rates to apply for a tax region
     * @param sourceId Optionally use this source for payment
     * @return ID of the invoice
     */
    fun createInvoice(customerId: String, taxRates: List<TaxRateInfo> = listOf<TaxRateInfo>(), sourceId: String? = null): Either<PaymentError, InvoiceInfo>

    /**
     * @param customerId ID of the customer to which the invoice will be assigned to
     * @param amount Amount to pay (an integer multiplied by 100)
     * @param currency The currency to use for the payment (ISO code)
     * @param description Short description of what the invoice is for
     * @param taxRegion An identifier representing the taxes to be applied to a region
     * @param sourceId Optionally use this source for payment
     * @return ID of the invoice
     */
    fun createInvoice(customerId: String, amount: Int, currency: String, description: String, taxRegionId: String?, sourceId: String?): Either<PaymentError, InvoiceInfo>

    /**
     * Pay an invoice now. If a 'sourceId' is given, then pay the invoice using
     * that source.
     * @param invoiceId ID of the invoice to be paid
     * @param sourceId Optionally the payment source to be used
     * @return ID of the invoice
     */
    fun payInvoice(invoiceId: String, sourceId: String? = null): Either<PaymentError, InvoicePaymentInfo>

    /**
     * @param invoiceId ID of the invoice to be paid
     * @return ID of the invoice
     */
    fun removeInvoice(invoiceId: String): Either<PaymentError, InvoiceInfo>

    fun createTaxRateForTaxRegionId(taxRegionId: String, percentage: BigDecimal, displayName: String, inclusive: Boolean = true): Either<PaymentError, TaxRateInfo>

    /**
     * @param region Region code
     * @return List with tax rates to apply for region if any found
     */
    fun getTaxRatesForTaxRegionId(taxRegionId: String?): Either<PaymentError, List<TaxRateInfo>>

    /**
     * Fetch payment transaction that lies within the time range 'start'..'end',
     * where the timestamps are Epoch timestamps in milliseconds.
     * @param start - lower timestamp range
     * @param end - upper timestamp range
     * @return payment transactions
     */
    fun getPaymentTransactions(start: Long, end: Long): Either<PaymentError, List<PaymentTransactionInfo>>
}
