package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import arrow.core.flatMap
import com.stripe.exception.*
import org.ostelco.prime.getLogger
import com.stripe.model.*
import org.ostelco.prime.paymentprocessor.core.*
import com.stripe.model.Customer

class StripePaymentProcessor : PaymentProcessor {

    private val logger by getLogger()

    override fun getSavedSources(customerId: String): Either<PaymentError, List<SourceDetailsInfo>> =
            either("Failed to retrieve sources for customer $customerId") {
                val sources = mutableListOf<SourceDetailsInfo>()
                val customer = Customer.retrieve(customerId)
                customer.sources.data.forEach {
                    val details = getAccountDetails(it)
                    sources.add(SourceDetailsInfo(it.id, getAccountType(details), details))
                }
                sources
//                sources.sortWith(Comparator<SourceDetailsInfo> {
//                    override fun compare(p1: SourceDetailsInfo, p2: SourceDetailsInfo) : Int when {
//                            p1.id > p2.id -> 1
//                            p1.id == p2.id -> 0
//                            else -> -1
//                    }
//                })
            }

    private fun getAccountType(details: Map<String, Any>) : String {
        return details.get("type").toString()
    }

    /* Returns detailed 'account details' for the given Stripe source/account.
       Note that including the fields 'id' and 'type' are manadatory. */
    private fun getAccountDetails(accountInfo: ExternalAccount) : Map<String, Any> {
        logger.info(">>> details: {}", accountInfo)
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
                             "created" to accountInfo.metadata.getOrElse("created") {
                                 "${System.currentTimeMillis() / 1000}"
                              },
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
                             "typeData" to accountInfo.typeData,
                             "owner" to accountInfo.owner)
            }
            else -> {
                logger.error("Received unsupported Stripe source/account type: {}",
                        accountInfo)
                return mapOf("id" to accountInfo.id,
                             "type" to "unsupported")
            }
        }
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
            if (customerList.data.isEmpty()) {
                 return Either.left(NotFoundError("Could not find a payment profile for user $userEmail"))
            } else if (customerList.data.size > 1){
                 return Either.left(NotFoundError("Multiple profiles for user $userEmail found"))
            } else {
                 return Either.right(ProfileInfo(customerList.data.first().id))
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
                        "metadata" to mapOf("created" to "${System.currentTimeMillis() / 1000}"))
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
                val item =  mapOf("plan" to planId)
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
        return either(errorMessage) {
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
        return either(errorMessage) {
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
            either("Failed to refund charge $chargeId") {
                val refundParams = mapOf("charge" to chargeId)
                Refund.create(refundParams).charge
            }

    override fun removeSource(customerId: String, sourceId: String): Either<PaymentError, String> =
            either("Failed to remove source $sourceId from customer $customerId") {
                Customer.retrieve(customerId).sources.retrieve(sourceId).delete().id
            }

    private fun <RETURN> either(errorDescription: String, action: () -> RETURN): Either<PaymentError, RETURN> {
        return try {
            Either.right(action())
        } catch (e: CardException) {
            // If something is decline with a card purchase, CardException will be caught
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.getCode()}", e)
            Either.left(ForbiddenError(errorDescription, e.message))
        } catch (e: RateLimitException) {
            // Too many requests made to the API too quickly
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.getCode()}", e)
            Either.left(BadGatewayError(errorDescription, e.message))
        } catch (e: InvalidRequestException) {
            // Invalid parameters were supplied to Stripe's API
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.getCode()}", e)
            Either.left(NotFoundError(errorDescription, e.message))
        } catch (e: AuthenticationException) {
            // Authentication with Stripe's API failed
            // (maybe you changed API keys recently)
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.getCode()}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: ApiConnectionException) {
            // Network communication with Stripe failed
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.getCode()}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: StripeException) {
            // Unknown Stripe error
            logger.error("Payment error : $errorDescription , Stripe Error Code: ${e.getCode()}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: Exception) {
            // Something else happened, could be completely unrelated to Stripe
            logger.error(errorDescription, e)
            Either.left(BadGatewayError(errorDescription))
        }
    }
}

