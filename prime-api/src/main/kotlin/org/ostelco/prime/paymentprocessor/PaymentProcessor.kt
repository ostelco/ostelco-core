package org.ostelco.prime.paymentprocessor

import io.vavr.control.Either
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
    fun addSource(customerId: String, sourceId: String):  Either<ApiError, SourceInfo>

    /**
     * @param userEmail: user email (Prime unique identifier for customer)
     * @return Stripe customerId if not created
     */
    fun createPaymentProfile(userEmail: String): Either<ApiError, ProfileInfo>

    /**
     * @param productId Stripe product id
     * @param amount The amount to be charged in the interval specified
     * @param currency Three-letter ISO currency code in lowercase
     * @param interval The frequency with which a subscription should be billed.
     * @return Stripe planId or null if not created
     */
    fun createPlan(productId: String, amount: Int, currency: String, interval: Interval): Either<ApiError, PlanInfo>

    /**
     * @param sku Prime product SKU
     * @return Stripe productId or null if not created
     */
    fun createProduct(sku: String): Either<ApiError, ProductInfo>

    /**
     * @param customerId Stripe customer id
     * @return List of Stripe sourceId or null if none stored
     */
    fun getSavedSources(customerId: String): Either<ApiError, List<SourceInfo>>

    /**
     * @param customerId Stripe customer id
     * @return Stripe default sourceId if created
     */
    fun getDefaultSource(customerId: String): Either<ApiError, SourceInfo>

    /**
     * @param customerId Stripe customer id
     * @return Stripe default chargeId or null failed
     */
    fun purchaseProduct(customerId: String, sourceId: String, amount: Int, currency: String, saveSource: Boolean = true): Either<ApiError, ProductInfo>

    /**
     * @param customerId Stripe customer id
     * @param sourceId Stripe source id
     * @return SourceInfo if created
     */
    fun setDefaultSource(customerId: String, sourceId: String): Either<ApiError, SourceInfo>

}