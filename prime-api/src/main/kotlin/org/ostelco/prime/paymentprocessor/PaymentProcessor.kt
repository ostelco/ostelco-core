package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import org.ostelco.prime.core.ApiError
import org.ostelco.prime.paymentprocessor.core.*

interface PaymentProcessor {

    enum class Interval(val value: String) {
        DAY("day"),
        WEEK("week"),
        MONTH("month"),
        YEAR("year")
    }

    /**
     * @param customerId Stripe customer id
     * @param sourceId Stripe source id
     * @return Stripe sourceId if created
     */
    fun addSource(customerId: String, sourceId: String): Either<PaymentError, SourceInfo>

    /**
     * @param userEmail: user email (Prime unique identifier for customer)
     * @return Stripe customerId if created
     */
    fun createPaymentProfile(userEmail: String): Either<PaymentError, ProfileInfo>

    /**
     * @param customerId Stripe customer id
     * @return Stripe customerId if deleted
     */
    fun deletePaymentProfile(customerId: String): Either<PaymentError, ProfileInfo>

    /**
     * @param productId Stripe product id
     * @param amount The amount to be charged in the interval specified
     * @param currency Three-letter ISO currency code in lowercase
     * @param interval The frequency with which a subscription should be billed.
     * @return Stripe planId if created
     */
    fun createPlan(productId: String, amount: Int, currency: String, interval: Interval): Either<PaymentError, PlanInfo>

    /**
     * @param Stripe Plan Id
     * @param Stripe Customer Id
     * @return Stripe SubscriptionId if subscribed
     */
    fun subscribeToPlan(planId: String, customerId: String): Either<PaymentError, SubscriptionInfo>

    /**
     * @param Stripe Plan Id
     * @return Stripe PlanId if deleted
     */
    fun removePlan(planId: String): Either<PaymentError, PlanInfo>

    /**
     * @param Stripe Subscription Id
     * @param Stripe atIntervalEnd set to true if the subscription shall remain active until the end of the Plan interval
     * @return Stripe SubscriptionId if unsubscribed
     */
    fun cancelSubscription(subscriptionId: String, atIntervalEnd: Boolean = true): Either<PaymentError, SubscriptionInfo>

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
     * @param customerId Stripe customer id
     * @return List of Stripe sourceId
     */
    fun getSavedSources(customerId: String): Either<PaymentError, List<SourceInfo>>

    /**
     * @param customerId Stripe customer id
     * @return Stripe default sourceId
     */
    fun getDefaultSource(customerId: String): Either<PaymentError, SourceInfo>

    /**
     * @param customerId Stripe customer id
     * @param sourceId Stripe source id
     * @return SourceInfo if created
     */
    fun setDefaultSource(customerId: String, sourceId: String): Either<PaymentError, SourceInfo>

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
    fun captureCharge(chargeId: String, customerId: String): Either<PaymentError, String>

    /**
     * @param chargeId ID of the of the authorized charge to refund from authorizeCharge()
     * @return id of the charge
     */
    fun refundCharge(chargeId: String): Either<PaymentError, String>

    /**
     * @param customerId Customer id in the payment system
     * @param sourceId id of the payment source
     * @return id if removed
     */
    fun removeSource(customerId: String, sourceId: String): Either<PaymentError, String>

}