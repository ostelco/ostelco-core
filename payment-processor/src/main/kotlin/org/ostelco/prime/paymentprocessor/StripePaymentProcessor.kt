package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import com.stripe.exception.ApiConnectionException
import com.stripe.exception.AuthenticationException
import com.stripe.exception.CardException
import com.stripe.exception.InvalidRequestException
import com.stripe.exception.RateLimitException
import com.stripe.exception.StripeException
import com.stripe.model.Card
import com.stripe.model.Charge
import com.stripe.model.Customer
import com.stripe.model.EphemeralKey
import com.stripe.model.ExternalAccount
import com.stripe.model.Plan
import com.stripe.model.Product
import com.stripe.model.Refund
import com.stripe.model.Source
import com.stripe.model.Subscription
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
import java.util.UUID


class StripePaymentProcessor : PaymentProcessor {

    private val logger by getLogger()

    override fun getSavedSources(customerId: String): Either<PaymentError, List<SourceDetailsInfo>> =
            either("Failed to retrieve sources for customer $customerId") {
                val customer = Customer.retrieve(customerId)
                val sources: List<SourceDetailsInfo> = customer.sources.data.map {
                    val details = getAccountDetails(it)
                    SourceDetailsInfo(it.id, getAccountType(details), details)
                }
                sources.sortedByDescending { it.details["created"] as Long }
            }

    private fun getAccountType(details: Map<String, Any>): String {
        return details["type"].toString()
    }

    /* Returns detailed 'account details' for the given Stripe source/account.
       Note that including the fields 'id', 'type' and 'created' are mandatory. */
    private fun getAccountDetails(accountInfo: ExternalAccount): Map<String, Any> {
        when (accountInfo) {
            is Card -> {
                return mapOf("id" to accountInfo.id,
                        "type" to "card",
                        "addressLine1" to accountInfo.addressLine1,
                        "addressLine2" to accountInfo.addressLine2,
                        "addressZip" to accountInfo.addressZip,
                        "addressCity" to accountInfo.addressCity,
                        "addressState" to accountInfo.addressState,
                        "brand" to accountInfo.brand,              // "Visa", "Mastercard" etc.
                        "country" to accountInfo.country,
                        "currency" to accountInfo.currency,
                        "cvcCheck" to accountInfo.cvcCheck,
                        "created" to getCreatedTimestampFromMetadata(accountInfo.id,
                                accountInfo.metadata),
                        "expMonth" to accountInfo.expMonth,
                        "expYear" to accountInfo.expYear,
                        "fingerprint" to accountInfo.fingerprint,
                        "funding" to accountInfo.funding,
                        "last4" to accountInfo.last4,              // Typ.: "credit" or "debit"
                        "threeDSecure" to accountInfo.threeDSecure)
                        .filterValues { it != null }
            }
            is Source -> {
                return mapOf("id" to accountInfo.id,
                        "type" to "source",
                        "created" to accountInfo.created,
                        "typeData" to accountInfo.typeData,
                        "owner" to accountInfo.owner)
            }
            else -> {
                logger.error("Received unsupported Stripe source/account type: {}",
                        accountInfo)
                return mapOf("id" to accountInfo.id,
                        "type" to "unsupported",
                        "created" to getSecondsSinceEpoch())
            }
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

    override fun createPaymentProfile(userEmail: String): Either<PaymentError, ProfileInfo> =
            either("Failed to create profile for user $userEmail") {
                val customerParams = mapOf("email" to userEmail)
                ProfileInfo(Customer.create(customerParams).id)
            }

    override fun getPaymentProfile(userEmail: String): Either<PaymentError, ProfileInfo> {
        val customerParams = mapOf(
                "limit" to "1",
                "email" to userEmail)
        val customerList = Customer.list(customerParams)
        return when {
            customerList.data.isEmpty() -> Either.left(NotFoundError("Could not find a payment profile for user $userEmail"))
            customerList.data.size > 1 -> Either.left(NotFoundError("Multiple profiles for user $userEmail found"))
            else -> Either.right(ProfileInfo(customerList.data.first().id))
        }
    }

    override fun createPlan(productId: String, amount: Int, currency: String, interval: PaymentProcessor.Interval): Either<PaymentError, PlanInfo> =
            either("Failed to create plan with product id $productId amount $amount currency $currency interval ${interval.value}") {
                val planParams = mapOf(
                        "amount" to amount,
                        "interval" to interval.value,
                        "product" to productId,
                        "currency" to currency)
                PlanInfo(Plan.create(planParams).id)
            }

    override fun removePlan(planId: String): Either<PaymentError, PlanInfo> =
            either("Failed to delete plan $planId") {
                val plan = Plan.retrieve(planId)
                PlanInfo(plan.delete().id)
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

    override fun addSource(customerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to add source $sourceId to customer $customerId") {
                val customer = Customer.retrieve(customerId)
                val params = mapOf("source" to sourceId,
                        "metadata" to mapOf("created" to getSecondsSinceEpoch()))
                SourceInfo(customer.sources.create(params).id)
            }

    override fun setDefaultSource(customerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to set default source $sourceId for customer $customerId") {
                val customer = Customer.retrieve(customerId)
                val updateParams = mapOf("default_source" to sourceId)
                val customerUpdated = customer.update(updateParams)
                SourceInfo(customerUpdated.defaultSource)
            }

    override fun getDefaultSource(customerId: String): Either<PaymentError, SourceInfo> =
            either("Failed to get default source for customer $customerId") {
                SourceInfo(Customer.retrieve(customerId).defaultSource)
            }

    override fun deletePaymentProfile(customerId: String): Either<PaymentError, ProfileInfo> =
            either("Failed to delete customer $customerId") {
                val customer = Customer.retrieve(customerId)
                ProfileInfo(customer.delete().id)
            }

    override fun subscribeToPlan(planId: String, customerId: String): Either<PaymentError, SubscriptionInfo> =
            either("Failed to subscribe customer $customerId to plan $planId") {
                val item = mapOf("plan" to planId)
                val params = mapOf(
                        "customer" to customerId,
                        "items" to mapOf("0" to item))

                SubscriptionInfo(Subscription.create(params).id)
            }

    override fun cancelSubscription(subscriptionId: String, atIntervalEnd: Boolean): Either<PaymentError, SubscriptionInfo> =
            either("Failed to unsubscribe subscription Id : $subscriptionId atIntervalEnd $atIntervalEnd") {
                val subscription = Subscription.retrieve(subscriptionId)
                val subscriptionParams = mapOf("at_period_end" to atIntervalEnd)
                SubscriptionInfo(subscription.cancel(subscriptionParams).id)
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

    override fun removeSource(customerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to remove source $sourceId from customer $customerId") {
                val accountInfo = Customer.retrieve(customerId).sources.retrieve(sourceId)
                when (accountInfo) {
                    is Card -> accountInfo.delete()
                    is Source -> accountInfo.detach()
                    else ->
                        Either.left(BadGatewayError("Attempt to remove unsupported account-type $accountInfo"))
                }
                SourceInfo(sourceId)
            }

    override fun getStripeEphemeralKey(userEmail: String, apiVersion: String): Either<PaymentError, String> =
            getPaymentProfile(userEmail)
                    .fold(
                            { createPaymentProfile(userEmail) },
                            { profileInfo -> profileInfo.right() }
                    ).flatMap { profileInfo ->
                        either("Failed to create stripe ephemeral key") {
                            EphemeralKey.create(
                                    mapOf("customer" to profileInfo.id),
                                    RequestOptions.builder().setStripeVersion(apiVersion).build())
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
            Either.left(NotFoundError(errorDescription, e.message))
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

