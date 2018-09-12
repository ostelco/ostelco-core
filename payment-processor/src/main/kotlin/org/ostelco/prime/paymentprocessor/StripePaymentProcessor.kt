package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import arrow.core.flatMap
import com.stripe.model.Charge
import com.stripe.model.Customer
import com.stripe.model.Plan
import com.stripe.model.Product
import com.stripe.model.Subscription
import org.ostelco.prime.logger
import com.stripe.model.Refund
import org.ostelco.prime.core.ApiErrorCode
import org.ostelco.prime.paymentprocessor.core.*


class StripePaymentProcessor : PaymentProcessor {

    private val logger by logger()

    override fun getSavedSources(customerId: String): Either<PaymentError, List<SourceInfo>> =
            either(NotFoundError("Failed to retrieve sources for customer $customerId")) {
                val sources = mutableListOf<SourceInfo>()
                val customer = Customer.retrieve(customerId)
                customer.sources.data.forEach {
                    sources.add(SourceInfo(it.id))
                }
                sources
            }

    override fun createPaymentProfile(userEmail: String): Either<PaymentError, ProfileInfo> =
            either(ForbiddenError("Failed to create profile for user $userEmail")) {
                val customerParams = mapOf("email" to userEmail)
                ProfileInfo(Customer.create(customerParams).id)
            }

    override fun createPlan(productId: String, amount: Int, currency: String, interval: PaymentProcessor.Interval): Either<PaymentError, PlanInfo> =
            either(ForbiddenError("Failed to create plan with product id $productId amount $amount currency $currency interval ${interval.value}")) {
                val planParams = mapOf(
                        "amount" to amount,
                        "interval" to interval.value,
                        "product" to productId,
                        "currency" to currency)
                PlanInfo(Plan.create(planParams).id)
            }

    override fun removePlan(planId: String): Either<PaymentError, PlanInfo> =
            either(NotFoundError("Failed to delete plan $planId")) {
                val plan = Plan.retrieve(planId)
                PlanInfo(plan.delete().id)
            }

    override fun createProduct(sku: String): Either<PaymentError, ProductInfo> =
            either(ForbiddenError("Failed to create product with sku $sku")) {
                val productParams = mapOf(
                        "name" to sku,
                        "type" to "service")
                ProductInfo(Product.create(productParams).id)
            }

    override fun removeProduct(productId: String): Either<PaymentError, ProductInfo> =
            either(NotFoundError("Failed to delete product $productId")) {
                val product = Product.retrieve(productId)
                ProductInfo(product.delete().id)
            }

    override fun addSource(customerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either(NotFoundError("Failed to add source $sourceId to customer $customerId")) {
                val customer = Customer.retrieve(customerId)
                val params = mapOf("source" to sourceId)
                SourceInfo(customer.sources.create(params).id)
            }

    override fun setDefaultSource(customerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either(NotFoundError("Failed to set default source $sourceId for customer $customerId")) {
                val customer = Customer.retrieve(customerId)
                val updateParams = mapOf("default_source" to sourceId)
                val customerUpdated = customer.update(updateParams)
                SourceInfo(customerUpdated.defaultSource)
            }

    override fun getDefaultSource(customerId: String): Either<PaymentError, SourceInfo> =
            either(NotFoundError("Failed to get default source for customer $customerId")) {
                SourceInfo(Customer.retrieve(customerId).defaultSource)
            }

    override fun deletePaymentProfile(customerId: String): Either<PaymentError, ProfileInfo> =
            either(NotFoundError("Failed to delete customer $customerId")) {
                val customer = Customer.retrieve(customerId)
                ProfileInfo(customer.delete().id)
            }

    override fun subscribeToPlan(planId: String, customerId: String): Either<PaymentError, SubscriptionInfo> =
            either(ForbiddenError("Failed to subscribe customer $customerId to plan $planId")) {
                val item =  mapOf("plan" to planId)
                val params = mapOf(
                        "customer" to customerId,
                        "items" to mapOf("0" to item))

                SubscriptionInfo(Subscription.create(params).id)
            }

    override fun cancelSubscription(subscriptionId: String, atIntervalEnd: Boolean): Either<PaymentError, SubscriptionInfo> =
            either(NotFoundError("Failed to unsubscribe subscription Id : $subscriptionId atIntervalEnd $atIntervalEnd")) {
                val subscription = Subscription.retrieve(subscriptionId)
                val subscriptionParams = mapOf("at_period_end" to atIntervalEnd)
                SubscriptionInfo(subscription.cancel(subscriptionParams).id)
            }


    override fun authorizeCharge(customerId: String, sourceId: String?, amount: Int, currency: String): Either<PaymentError, String> {
        val errorMessage = "Failed to authorize the charge for customerId $customerId sourceId $sourceId amount $amount currency $currency"
        return either(ForbiddenError(errorMessage)) {
            val chargeParams = mutableMapOf(
                    "amount" to amount,
                    "currency" to currency,
                    "customer" to customerId,
                    "capture" to false)
            if (sourceId != null) {
                chargeParams["source"] = sourceId
            }
            Charge.create(chargeParams)
        }.flatMap { charge: Charge ->
            val review = charge.review
            Either.cond(
                    test = (review == null),
                    ifTrue = { charge.id },
                    ifFalse = { ForbiddenError("Review required, $errorMessage $review") }
            )
        }
    }

    override fun captureCharge(chargeId: String, customerId: String): Either<PaymentError, String> {
        val errorMessage = "Failed to capture charge for customerId $customerId chargeId $chargeId"
        return either(NotFoundError(errorMessage)) {
            Charge.retrieve(chargeId)
        }.flatMap { charge: Charge ->
            val review = charge.review
            Either.cond(
                    test = (review == null),
                    ifTrue = { charge },
                    ifFalse = { ForbiddenError("Review required, $errorMessage $review") }
            )
        }.flatMap { charge ->
            try {
                charge.capture()
                Either.right(charge.id)
            } catch (e: Exception) {
                logger.warn(errorMessage, e)
                Either.left(BadGatewayError(errorMessage))
            }
        }
    }

    override fun refundCharge(chargeId: String): Either<PaymentError, String> =
            either(NotFoundError("Failed to refund charge $chargeId")) {
                val refundParams = mapOf("charge" to chargeId)
                Refund.create(refundParams).charge
            }

    override fun removeSource(customerId: String, sourceId: String): Either<PaymentError, String> =
            either(ForbiddenError("Failed to remove source $sourceId from customer $customerId")) {
                Customer.retrieve(customerId).sources.retrieve(sourceId).delete().id
            }

    private fun <RETURN> either(paymentError: PaymentError, action: () -> RETURN): Either<PaymentError, RETURN> {
        return try {
            Either.right(action())
        } catch (e: Exception) {
            logger.warn(paymentError.description, e)
            paymentError.externalErrorMessage = e.message
            Either.left(paymentError)
        }
    }
}

