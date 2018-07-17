package org.ostelco.prime.paymentprocessor

import io.vavr.control.Either
import org.ostelco.prime.paymentprocessor.core.ApiError
import org.ostelco.prime.paymentprocessor.core.SourceInfo

/**
 *
 */
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
     * @return Stripe sourceId or null if not created
     */
    fun addSource(customerId: String, sourceId: String): String?

    /**
     * @param userEmail: user email (Prime unique identifier for customer)
     * @return Stripe customerId or null if not created
     */
    fun createPaymentProfile(userEmail: String): String?

    /**
     * @param productId Stripe product id
     * @param amount The amount to be charged in the interval specified
     * @param currency Three-letter ISO currency code in lowercase
     * @param interval The frequency with which a subscription should be billed.
     * @return Stripe planId or null if not created
     */
    fun createPlan(productId: String, amount: Int, currency: String, interval: Interval): String?

    /**
     * @param sku Prime product SKU
     * @return Stripe productId or null if not created
     */
    fun createProduct(sku: String): String?

    /**
     * @param customerId Stripe customer id
     * @return List of Stripe sourceId or null if none stored
     */
    fun getSavedSources(customerId: String): List<String>

    /**
     * @param customerId Stripe customer id
     * @return Stripe default sourceId or null if not set
     */
    fun getDefaultSource(customerId: String): String?

    /**
     * @param customerId Stripe customer id
     * @return Stripe default chargeId or null failed
     */
    fun purchaseProduct(customerId: String, sourceId: String, amount: Int, currency: String): String?

    /**
     * @param customerId Stripe customer id
     * @param sourceId Stripe source id
     * @return Stripe customerId or null if not created
     */
    fun setDefaultSource(customerId: String, sourceId: String): String?

}
/*
    fun createSource(customerId: String, sourceId: String): Either<ApiError, SourceInfo>

    fun getSavedSources(customerId: String): Either<ApiError, Collection<SourceInfo>>

    fun setDefaultSource(customerId: String, sourceId: String): Either<ApiError, SourceInfo>

    fun createProfile(userEmail: String): String?
*/
