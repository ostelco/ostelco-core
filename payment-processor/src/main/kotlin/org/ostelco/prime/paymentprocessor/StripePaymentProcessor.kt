package org.ostelco.prime.paymentprocessor

import com.stripe.model.*
import io.vavr.control.Either
import org.ostelco.prime.logger
import org.ostelco.prime.core.ApiError
import org.ostelco.prime.paymentprocessor.core.*

class StripePaymentProcessor : PaymentProcessor {

    private val LOG by logger()

    override fun getSavedSources(customerId: String): Either<ApiError, List<SourceInfo>> =
            either ("Failed to get sources for customer ${customerId}") {
                var sources = mutableListOf<SourceInfo>()
                val customer = Customer.retrieve(customerId)
                customer.sources.data.forEach {
                    sources.add(SourceInfo(it.id))
                }
                sources
            }

    override fun createPaymentProfile(userEmail: String): Either<ApiError, ProfileInfo> =
            either(errorMessage = "Failed to create profile for user ${userEmail}") {
                val customerParams = HashMap<String, Any>()
                customerParams.put("email", userEmail)
                ProfileInfo(Customer.create(customerParams).id)
            }

    override fun createPlan(productId: String, amount: Int, currency: String, interval: PaymentProcessor.Interval): Either<ApiError, PlanInfo> =
            either(errorMessage = "Failed to create plan with producuct id ${productId} amount ${amount} currency ${currency} interval ${interval.value}") {
                val productParams = HashMap<String, Any>()
                productParams["name"] = "Quartz pro"

                val planParams = HashMap<String, Any>()
                planParams["amount"] = amount
                planParams["interval"] = interval.value
                planParams["product"] = productId
                planParams["currency"] = currency
                PlanInfo(Plan.create(planParams).id)
            }

    override fun createProduct(sku: String): Either<ApiError, ProductInfo> =
            either(errorMessage = "Failed to create product with sku ${sku}") {
                val productParams = HashMap<String, Any>()
                productParams["name"] = sku
                productParams["type"] = "service"
                ProductInfo(Product.create(productParams).id)
            }

    override fun addSource(customerId: String, sourceId: String): Either<ApiError, SourceInfo> =
            either(errorMessage = "Failed to add source ${sourceId} to customer ${customerId}") {
                val customer = Customer.retrieve(customerId)
                val params = HashMap<String, Any>()
                params["source"] = sourceId
                SourceInfo(customer.sources.create(params).id)
            }

    override fun setDefaultSource(customerId: String, sourceId: String): Either<ApiError, SourceInfo> =
            either(errorMessage = "Failed to set default source ${sourceId} for customer ${customerId}") {
                val customer = Customer.retrieve(customerId)
                val updateParams = HashMap<String, Any>()
                updateParams.put("default_source", sourceId)
                val customerUpdated = customer.update(updateParams)
                SourceInfo(customerUpdated.id)
            }

    override fun getDefaultSource(customerId: String): Either<ApiError, SourceInfo> =
            either(errorMessage = "Failed to get default source for customer ${customerId}") {
                SourceInfo(Customer.retrieve(customerId).defaultSource)
            }

    // FixMe : This needs to be able to rollback
    override fun purchaseProduct(customerId: String, sourceId: String, amount: Int, currency: String, saveCard: Boolean): Either<ApiError, ProductInfo> {

        var storedSourceId = sourceId
        val stored = isSourceStored(customerId, sourceId)
        if (stored.isLeft) {
            return Either.left(stored.left)
        } else if (!stored.right().get()) {
            val savedSource = addSource(customerId, sourceId)
            if (savedSource.isLeft) {
                return Either.left(savedSource.left)
            } else {
                storedSourceId = savedSource.right().get().id
            }
        }

        val charge = purchaseProduct(customerId, storedSourceId, amount, currency)
        if (charge.isLeft) {
            return Either.left(charge.left)
        }

        if (!saveCard) {
            val removed = removeSource(customerId, storedSourceId)
            if (removed.isLeft) {
                return Either.left(removed.left)
            }
        }

        return Either.right(ProductInfo(charge.right().get().id))
    }

    private fun purchaseProduct(customerId: String, sourceId: String, amount: Int, currency: String): Either<ApiError, ProductInfo> =
            either(errorMessage = "Failed to purchase product customerId ${customerId} sourceId ${sourceId} amount ${amount} currency ${currency}") {
                val chargeParams = HashMap<String, Any>()
                chargeParams["amount"] = amount
                chargeParams["currency"] = currency
                chargeParams["customer"] = customerId
                chargeParams["source"] = sourceId

                val charge = Charge.create(chargeParams)
                ProductInfo(charge.id)
            }

    private fun isSourceStored(customerId: String, sourceId: String): Either<ApiError, Boolean> {
        val storedSources = getSavedSources(customerId)
        if (storedSources.isLeft) {
            return Either.left(storedSources.left)
        }
        var sourceStored = false
        storedSources.right().get().forEach {
            if (it.id.equals(sourceId)) {
                sourceStored = true
            }
        }
        return Either.right(sourceStored)
    }

    private fun removeSource(customerId: String, sourceId: String): Either<ApiError, SourceInfo> =
            either("Failed to remove source ${sourceId} from customer ${customerId}") {
                SourceInfo(Customer.retrieve(customerId).sources.retrieve(sourceId).delete().id)
            }

    private fun <RETURN> either(errorMessage: String, action: () -> RETURN): Either<ApiError, RETURN> {
        return try {
            Either.right(action())
        } catch (e: Exception) {
            LOG.warn(errorMessage, e)
            Either.left(ApiError(errorMessage))
        }
    }
}
