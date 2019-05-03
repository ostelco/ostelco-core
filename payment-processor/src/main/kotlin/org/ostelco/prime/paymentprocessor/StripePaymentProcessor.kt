package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import arrow.core.right
import com.stripe.exception.ApiConnectionException
import com.stripe.exception.AuthenticationException
import com.stripe.exception.CardException
import com.stripe.exception.InvalidRequestException
import com.stripe.exception.RateLimitException
import com.stripe.exception.StripeException
import com.stripe.model.*
import com.stripe.net.RequestOptions
import org.ostelco.prime.getLogger
import org.ostelco.prime.paymentprocessor.core.BadGatewayError
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.NotFoundError
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.PlanInfo
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import org.ostelco.prime.paymentprocessor.core.SourceDetailsInfo
import org.ostelco.prime.paymentprocessor.core.SourceInfo
import org.ostelco.prime.paymentprocessor.core.SubscriptionInfo
import java.time.Instant
import java.util.*


class StripePaymentProcessor : PaymentProcessor {

    private val logger by getLogger()

    override fun getSavedSources(stripeCustomerId: String): Either<PaymentError, List<SourceDetailsInfo>> =
            either("Failed to retrieve sources for customer $stripeCustomerId") {
                val customer = Customer.retrieve(stripeCustomerId)
                val sources: List<SourceDetailsInfo> = customer.sources.data.map {
                    val details = getAccountDetails(it)
                    SourceDetailsInfo(it.id, getAccountType(details), details)
                }
                sources.sortedByDescending { it.details["created"] as Long }
            }

    private fun getAccountType(details: Map<String, Any>): String {
        return details["type"].toString()
    }

    /* Returns 'source' details for the given Stripe source/account.
       Note that including the fields 'id', 'type' and 'created' are mandatory. */
    private fun getAccountDetails(paymentSource: PaymentSource): Map<String, Any> =
            when (paymentSource) {
                is Card -> {
                    mapOf("id" to paymentSource.id,
                            "type" to "card",
                            "addressLine1" to paymentSource.addressLine1,
                            "addressLine2" to paymentSource.addressLine2,
                            "addressZip" to paymentSource.addressZip,
                            "addressCity" to paymentSource.addressCity,
                            "addressState" to paymentSource.addressState,
                            "brand" to paymentSource.brand,              // "Visa", "Mastercard" etc.
                            "country" to paymentSource.country,
                            "currency" to paymentSource.currency,
                            "cvcCheck" to paymentSource.cvcCheck,
                            "created" to getCreatedTimestampFromMetadata(paymentSource.id,
                                    paymentSource.metadata),
                            "expMonth" to paymentSource.expMonth,
                            "expYear" to paymentSource.expYear,
                            "fingerprint" to paymentSource.fingerprint,
                            "funding" to paymentSource.funding,
                            "last4" to paymentSource.last4)              // Typ.: "credit" or "debit"
                            .filterValues { it != null }
                }
                is Source -> {
                    mapOf("id" to paymentSource.id,
                            "type" to "source",
                            "created" to paymentSource.created,
                            "owner" to paymentSource.owner,
                            "threeDSecure" to paymentSource.threeDSecure)
                }
                else -> {
                    logger.error("Received unsupported Stripe source/account type: {}",
                            paymentSource)
                    mapOf("id" to paymentSource.id,
                            "type" to "unsupported",
                            "created" to getSecondsSinceEpoch())
                }
            }

    /* Handle type conversion when reading the 'created' field from the
       metadata returned from Stripe. (It might seem like that Stripe
       returns stored metadata values as strings, even if they where stored
       using an another type. Needs to be verified.) */
    private fun getCreatedTimestampFromMetadata(id: String, metadata: Map<String, Any>): Long {
        val created: String? = metadata["created"] as? String
        return created?.toLongOrNull() ?: run {
            logger.warn("No 'created' timestamp found in metadata for Stripe account {}",
                    id)
            getSecondsSinceEpoch()
        }
    }

    /* Seconds since Epoch in UTC zone. */
    private fun getSecondsSinceEpoch(): Long {
        return System.currentTimeMillis() / 1000L
    }

    override fun createPaymentProfile(customerId: String, email: String): Either<PaymentError, ProfileInfo> =
            either("Failed to create profile for user $customerId") {
                val customerParams = mapOf(
                        "id" to customerId,
                        "email" to email,
                        "metadata" to mapOf("customerId" to customerId))
                ProfileInfo(Customer.create(customerParams).id)
            }

    override fun getPaymentProfile(customerId: String): Either<PaymentError, ProfileInfo> =
            Try {
                Customer.retrieve(customerId)
            }.fold(
                    ifSuccess = { customer ->
                        when {
                            customer.deleted == true -> Either.left(NotFoundError("Payment profile for user $customerId was previously deleted"))
                            else -> Either.right(ProfileInfo(customer.id))
                        }
                    },
                    ifFailure = {
                        Either.left(NotFoundError("Could not find a payment profile for user $customerId"))
                    }
            )

    override fun createPlan(productId: String, amount: Int, currency: String, interval: PaymentProcessor.Interval, intervalCount: Long): Either<PaymentError, PlanInfo> =
            either("Failed to create plan for product $productId amount $amount currency $currency interval ${interval.value}") {
                val planParams = mapOf(
                        "product" to productId,
                        "amount" to amount,
                        "interval" to interval.value,
                        "interval_count" to intervalCount,
                        "currency" to currency)
                val plan = Plan.create(planParams)
                PlanInfo(plan.id)
            }

    override fun removePlan(planId: String): Either<PaymentError, PlanInfo> =
            either("Failed to delete plan $planId") {
                Plan.retrieve(planId).let { plan ->
                    plan.delete()
                    PlanInfo(plan.id)
                }
            }

    override fun createProduct(sku: String): Either<PaymentError, ProductInfo> =
            either("Failed to create product with sku $sku") {
                val productParams = mapOf(
                        "name" to sku,
                        "type" to "service")
                ProductInfo(Product.create(productParams).id)
            }

    override fun removeProduct(productId: String): Either<PaymentError, ProductInfo> =
            either("Failed to delete product $productId") {
                val product = Product.retrieve(productId)
                ProductInfo(product.delete().id)
            }

    override fun addSource(stripeCustomerId: String, stripeSourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to add source $stripeSourceId to customer $stripeCustomerId") {
                val customer = Customer.retrieve(stripeCustomerId)
                val sourceParams = mapOf("source" to stripeSourceId,
                        "metadata" to mapOf("created" to getSecondsSinceEpoch()))
                SourceInfo(customer.sources.create(sourceParams).id)
            }

    override fun setDefaultSource(stripeCustomerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to set default source $sourceId for customer $stripeCustomerId") {
                val customer = Customer.retrieve(stripeCustomerId)
                val updateParams = mapOf("default_source" to sourceId)
                val customerUpdated = customer.update(updateParams)
                SourceInfo(customerUpdated.defaultSource)
            }

    override fun getDefaultSource(stripeCustomerId: String): Either<PaymentError, SourceInfo> =
            either("Failed to get default source for customer $stripeCustomerId") {
                SourceInfo(Customer.retrieve(stripeCustomerId).defaultSource)
            }

    override fun deletePaymentProfile(stripeCustomerId: String): Either<PaymentError, ProfileInfo> =
            either("Failed to delete customer $stripeCustomerId") {
                val customer = Customer.retrieve(stripeCustomerId)
                ProfileInfo(customer.delete().id)
            }

    override fun createSubscription(planId: String, stripeCustomerId: String, trialEnd: Long): Either<PaymentError, SubscriptionInfo> =
            either("Failed to subscribe customer $stripeCustomerId to plan $planId") {
                val item =  mapOf("plan" to planId)
                val subscriptionParams = mapOf(
                        "customer" to stripeCustomerId,
                        "items" to mapOf("0" to item),
                        *( if (trialEnd > Instant.now().epochSecond)
                               arrayOf("trial_end" to trialEnd.toString())
                           else
                               arrayOf()) )
                val subscription = Subscription.create(subscriptionParams)
                SubscriptionInfo(subscription.id, subscription.created, subscription.trialEnd ?: 0L)
            }

    override fun cancelSubscription(subscriptionId: String, atIntervalEnd: Boolean): Either<PaymentError, SubscriptionInfo> =
            either("Failed to unsubscribe subscription Id : $subscriptionId atIntervalEnd $atIntervalEnd") {
                val subscription = Subscription.retrieve(subscriptionId)
                val subscriptionParams = mapOf("at_period_end" to atIntervalEnd)
                subscription.cancel(subscriptionParams)
                SubscriptionInfo(subscription.id, subscription.created, subscription.trialEnd ?: 0L)
            }

    override fun authorizeCharge(customerId: String, sourceId: String?, amount: Int, currency: String): Either<PaymentError, String> {
        val errorMessage = "Failed to authorize the charge for customerId $customerId sourceId $sourceId amount $amount currency $currency"
        return when (amount) {
            0 -> Either.right("ZERO_CHARGE_${UUID.randomUUID()}")
            else -> either(errorMessage) {
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
    }

    override fun captureCharge(chargeId: String, customerId: String, amount: Int, currency: String): Either<PaymentError, String> {
        val errorMessage = "Failed to capture charge for customerId $customerId chargeId $chargeId"
        return when (amount) {
            0 -> Either.right(chargeId)
            else -> either(errorMessage) {
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
    }

    override fun refundCharge(chargeId: String, amount: Int, currency: String): Either<PaymentError, String> =
            when (amount) {
                0 -> Either.right(chargeId)
                else -> either("Failed to refund charge $chargeId") {
                    val refundParams = mapOf("charge" to chargeId)
                    Refund.create(refundParams).id
                }
            }

    override fun removeSource(stripeCustomerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to remove source $sourceId for stripeCustomerId $stripeCustomerId") {
                val accountInfo = Customer.retrieve(stripeCustomerId).sources.retrieve(sourceId)
                when (accountInfo) {
                    is Card -> accountInfo.delete()
                    is Source -> accountInfo.detach()
                    else ->
                        Either.left(BadGatewayError("Attempt to remove unsupported account-type $accountInfo"))
                }
                SourceInfo(sourceId)
            }

    override fun getStripeEphemeralKey(customerId: String, email: String, apiVersion: String): Either<PaymentError, String> =
            getPaymentProfile(customerId)
                    .fold(
                            { createPaymentProfile(customerId, email) },
                            { profileInfo -> profileInfo.right() }
                    ).flatMap { profileInfo ->
                        either("Failed to create stripe ephemeral key") {
                            EphemeralKey.create(
                                    mapOf("customer" to profileInfo.id),
                                    RequestOptions.builder().setStripeVersionOverride(apiVersion).build())
                                    .rawJson
                        }
                    }


    private fun <RETURN> either(errorDescription: String, action: () -> RETURN): Either<PaymentError, RETURN> {
        return try {
            Either.right(action())
        } catch (e: CardException) {
            // If something is decline with a card purchase, CardException will be caught
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.code}", e)
            Either.left(ForbiddenError(errorDescription, e.message))
        } catch (e: RateLimitException) {
            // Too many requests made to the API too quickly
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.code}", e)
            Either.left(BadGatewayError(errorDescription, e.message))
        } catch (e: InvalidRequestException) {
            // Invalid parameters were supplied to Stripe's API
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.code}", e)
            Either.left(ForbiddenError(errorDescription, e.message))
        } catch (e: AuthenticationException) {
            // Authentication with Stripe's API failed
            // (maybe you changed API keys recently)
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.code}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: ApiConnectionException) {
            // Network communication with Stripe failed
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.code}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: StripeException) {
            // Unknown Stripe error
            logger.error("Payment error : $errorDescription , Stripe Error Code: ${e.code}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: Exception) {
            // Something else happened, could be completely unrelated to Stripe
            logger.error(errorDescription, e)
            Either.left(BadGatewayError(errorDescription))
        }
    }
}
