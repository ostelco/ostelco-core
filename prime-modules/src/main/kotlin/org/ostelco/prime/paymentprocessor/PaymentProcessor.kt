package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.PlanInfo
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import org.ostelco.prime.paymentprocessor.core.SourceDetailsInfo
import org.ostelco.prime.paymentprocessor.core.SourceInfo
import org.ostelco.prime.paymentprocessor.core.SubscriptionDetailsInfo
import org.ostelco.prime.paymentprocessor.core.SubscriptionInfo
import org.ostelco.prime.paymentprocessor.core.TaxRateInfo

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
     * @param Stripe Plan Id
     * @param stripeCustomerId Stripe Customer Id
     * @param Epoch timestamp for when the trial period ends
     * @return Stripe SubscriptionId if subscribed
     */
    fun createSubscription(planId: String, stripeCustomerId: String, trialEnd: Long = 0L): Either<PaymentError, SubscriptionDetailsInfo>

    /**
     * @param Stripe Subscription Id
     * @param Stripe invoiceNow set to true if a final invoice should be generated
     * @return Stripe SubscriptionId if unsubscribed
     */
    fun cancelSubscription(subscriptionId: String, invoiceNow: Boolean = false): Either<PaymentError, SubscriptionInfo>

    /**
     * @param sku Prime product SKU
     * @return Stripe productId if created
     */
    fun createProduct(sku: String): Either<PaymentError, ProductInfo>

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
    fun refundCharge(chargeId: String, amount: Int, currency: String
    ): Either<PaymentError, String>

    /**
     * @param stripeCustomerId Customer id in the payment system
     * @param sourceId id of the payment source
     * @return id if removed
     */
    fun removeSource(stripeCustomerId: String, sourceId: String): Either<PaymentError, SourceInfo>

    fun getStripeEphemeralKey(customerId: String, email: String, apiVersion: String): Either<PaymentError, String>

    /**
     * @param region Region code
     * @return List with tax rates to apply for region if any found
     */
    fun getTaxRateForRegion(region: String): Either<PaymentError, List<TaxRateInfo>>
}