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

    override fun purchaseProduct(customerId: String, sourceId: String, amount: Int, currency: String, saveCard: Boolean): Either<ApiError, ProductInfo> =
            either(errorMessage = "Failed to purchase product customerId ${customerId} sourceId ${sourceId} amount ${amount} currency ${currency}") {
                val chargeParams = HashMap<String, Any>()
                chargeParams["amount"] = amount
                chargeParams["currency"] = currency
                chargeParams["customer"] = customerId
                chargeParams["source"] = sourceId

                ProductInfo(Charge.create(chargeParams).id)
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
