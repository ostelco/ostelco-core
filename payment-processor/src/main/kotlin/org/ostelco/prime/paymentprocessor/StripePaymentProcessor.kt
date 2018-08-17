package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import arrow.core.flatMap
import com.stripe.model.Charge
import com.stripe.model.Customer
import com.stripe.model.Plan
import com.stripe.model.Product
import com.stripe.model.Subscription
import org.ostelco.prime.core.ApiError
import org.ostelco.prime.core.BadGatewayError
import org.ostelco.prime.core.ForbiddenError
import org.ostelco.prime.core.NotFoundError
import org.ostelco.prime.logger
import org.ostelco.prime.paymentprocessor.core.*

class StripePaymentProcessor : PaymentProcessor {

    private val LOG by logger()

    override fun getSavedSources(customerId: String): Either<ApiError, List<SourceInfo>> =
            either (NotFoundError("Failed to get sources for customer ${customerId}")) {
                val sources = mutableListOf<SourceInfo>()
                val customer = Customer.retrieve(customerId)
                customer.sources.data.forEach {
                    sources.add(SourceInfo(it.id))
                }
                sources
            }

    override fun createPaymentProfile(userEmail: String): Either<ApiError, ProfileInfo> =
            either(ForbiddenError("Failed to create profile for user ${userEmail}")) {
                val customerParams = HashMap<String, Any>()
                customerParams.put("email", userEmail)
                ProfileInfo(Customer.create(customerParams).id)
            }

    override fun createPlan(productId: String, amount: Int, currency: String, interval: PaymentProcessor.Interval): Either<ApiError, PlanInfo> =
            either(ForbiddenError("Failed to create plan with product id ${productId} amount ${amount} currency ${currency} interval ${interval.value}")) {
                val planParams = HashMap<String, Any>()
                planParams["amount"] = amount
                planParams["interval"] = interval.value
                planParams["product"] = productId
                planParams["currency"] = currency
                PlanInfo(Plan.create(planParams).id)
            }

    override fun createProduct(sku: String): Either<ApiError, ProductInfo> =
            either(ForbiddenError("Failed to create product with sku ${sku}")) {
                val productParams = HashMap<String, Any>()
                productParams["name"] = sku
                productParams["type"] = "service"
                ProductInfo(Product.create(productParams).id)
            }

    override fun addSource(customerId: String, sourceId: String): Either<ApiError, SourceInfo> =
            either(ForbiddenError("Failed to add source ${sourceId} to customer ${customerId}")) {
                val customer = Customer.retrieve(customerId)
                val params = HashMap<String, Any>()
                params["source"] = sourceId
                SourceInfo(customer.sources.create(params).id)
            }

    override fun setDefaultSource(customerId: String, sourceId: String): Either<ApiError, SourceInfo> =
            either(ForbiddenError("Failed to set default source ${sourceId} for customer ${customerId}")) {
                val customer = Customer.retrieve(customerId)
                val updateParams = HashMap<String, Any>()
                updateParams.put("default_source", sourceId)
                val customerUpdated = customer.update(updateParams)
                SourceInfo(customerUpdated.defaultSource)
            }

    override fun getDefaultSource(customerId: String): Either<ApiError, SourceInfo> =
            either(NotFoundError( "Failed to get default source for customer ${customerId}")) {
                SourceInfo(Customer.retrieve(customerId).defaultSource)
            }

    override fun deletePaymentProfile(customerId: String): Either<ApiError, ProfileInfo> =
            either(NotFoundError("Failed to delete customer ${customerId}")) {
                val customer = Customer.retrieve(customerId)
                ProfileInfo(customer.delete().id)
            }

    override fun subscribeToPlan(planId: String, customerId: String): Either<ApiError, SubscriptionInfo> =
            either(ForbiddenError("Failed to subscribe customer ${customerId} to plan ${planId}")) {
                val item = HashMap<String, Any>()
                item["plan"] = planId

                val items = HashMap<String, Any>()
                items["0"] = item

                val params = HashMap<String, Any>()
                params["customer"] = customerId
                params["items"] = items

                SubscriptionInfo(Subscription.create(params).id)
            }

    override fun cancelSubscription(subscriptionId: String, atIntervalEnd: Boolean): Either<ApiError, SubscriptionInfo> =
            either(ForbiddenError("Failed to unsubscribe subscription Id : ${subscriptionId} atIntervalEnd ${atIntervalEnd}")) {
                val subscription = Subscription.retrieve(subscriptionId)
                val subscriptionParams = HashMap<String, Any>()
                subscriptionParams["at_period_end"] = atIntervalEnd
                SubscriptionInfo(subscription.cancel(subscriptionParams).id)
            }

    private fun chargeCustomer(customerId: String, sourceId: String?, amount: Int, currency: String): Either<ApiError, ProductInfo> =
            either(ForbiddenError("Failed to charge customer, customerId ${customerId} sourceId ${sourceId} amount ${amount} currency ${currency}")) {
                val chargeParams = HashMap<String, Any>()
                chargeParams["amount"] = amount
                chargeParams["currency"] = currency
                chargeParams["customer"] = customerId
                if (sourceId != null) {
                    chargeParams["source"] = sourceId
                }

                val charge = Charge.create(chargeParams)
                ProductInfo(charge.id)
            }


    override fun authorizeCharge(customerId: String, sourceId: String?, amount: Int, currency: String): Either<ApiError, String> {
        val errorMessage = "Failed to authorize the charge for customerId $customerId sourceId $sourceId amount $amount currency $currency"
        return either(ForbiddenError(errorMessage)) {
            val chargeParams = HashMap<String, Any>()
            chargeParams["amount"] = amount
            chargeParams["currency"] = currency
            chargeParams["customer"] = customerId
            chargeParams["capture"] = false
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

    override fun captureCharge(chargeId: String, customerId: String, sourceId: String?): Either<ApiError, String> {
        val errorMessage = "Failed to capture charge for customerId $customerId chargeId $chargeId"
        return either(ForbiddenError(errorMessage)) {
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
                LOG.warn(errorMessage, e)
                Either.left(BadGatewayError(errorMessage))
            }
        }
    }

    override fun removeSource(customerId: String, sourceId: String): Either<ApiError, String> =
            either(ForbiddenError("Failed to remove source ${sourceId} from customer ${customerId}")) {
                Customer.retrieve(customerId).sources.retrieve(sourceId).delete().id
            }

    private fun isSourceStored(customerId: String, sourceId: String): Either<ApiError, Boolean> =
            getSavedSources(customerId).map { sourceInfoList -> sourceInfoList.find { it.id.equals(sourceId) } != null }

    private fun <RETURN> either(apiError: ApiError, action: () -> RETURN): Either<ApiError, RETURN> {
        return try {
            Either.right(action())
        } catch (e: Exception) {
            LOG.warn(apiError.description, e)
            Either.left(apiError)
        }
    }
}
