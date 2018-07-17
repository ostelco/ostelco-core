package org.ostelco.prime.paymentprocessor

import com.stripe.model.Customer
import com.stripe.model.Plan
import com.stripe.model.Product
import org.ostelco.prime.logger

class StripePaymentProcessor : PaymentProcessor {

    private val LOG by logger()

    /**
     * @param customerId Stripe customer id
     * @return List of Stripe sourceId or null if none stored
     */
    override fun getSavedSources(customerId: String): List<String> {
        val customer = Customer.retrieve(customerId)
        println("Customer is : ${customer}")
        println("Customer has source : ${customer.sources.data}")
        return emptyList()
    }

    /**
     * @param userEmail: user email (Prime unique identifier for customer)
     * @return Stripe customerId or null if not created
     */
    override fun createPaymentProfile(userEmail: String): String? {
        val customerParams = HashMap<String, Any>()
        customerParams.put("email", userEmail)
        return try {
            Customer.create(customerParams)?.id
        } catch (e: Exception) {
            LOG.warn("Failed to create profile for user ${userEmail}", e)
            null
        }
    }

    /**
     * @param productId Stripe product id
     * @param amount The amount to be charged in the interval specified
     * @param currency Three-letter ISO currency code in lowercase
     * @param interval The frequency with which a subscription should be billed.
     * @return Stripe planId or null if not created
     */
    override fun createPlan(productId: String, amount: Int, currency: String, interval: PaymentProcessor.Interval) : String?{
        val productParams = HashMap<String, Any>()
        productParams["name"] = "Quartz pro"

        val planParams = HashMap<String, Any>()
        planParams["amount"] = amount
        planParams["interval"] = interval.value
        planParams["product"] = productId
        planParams["currency"] = currency

        return try {
            Plan.create(planParams)?.id
        } catch (e: Exception) {
            LOG.warn("Failed to create plan with producuct id ${productId} amount ${amount} currency ${currency} interval ${interval.value}", e)
            null
        }
    }

    /**
     * @param sku Prime product SKU
     * @return Stripe productId or null if not created
     */
    override fun createProduct(sku: String): String? {
        val productParams = HashMap<String, Any>()
        productParams["name"] = sku
        productParams["type"] = "service"

        return try {
            Product.create(productParams)?.id
        } catch (e : Exception) {
            LOG.warn("Failed to create product with sku ${sku}", e)
            null
        }
    }

    /**
     * @param customerId Stripe customer id
     * @param sourceId Stripe source id
     * @return Stripe sourceId or null if not created
     */
    override fun addSource(customerId: String, sourceId: String): String? {
        return try {
            val customer = Customer.retrieve(customerId)
            val params = HashMap<String, Any>();
            params["source"] = sourceId;
            customer?.sources?.create(params)?.id
        } catch (e: Exception) {
            LOG.warn("Failed to add source ${sourceId} to customer ${customerId}", e)
            null
        }
    }

    /**
     * @param customerId Stripe customer id
     * @param sourceId Stripe source id
     * @return Stripe customerId or null if not created
     */
    override fun setDefaultSource(customerId: String, sourceId: String): String? {
        return try {
            val customer = Customer.retrieve(customerId);
            val updateParams = HashMap<String, Any>();
            updateParams.put("default_source", sourceId);
            customer?.update(updateParams)?.id
        } catch (e: Exception) {
            LOG.warn("Failed to set default source ${sourceId} for customer ${customerId}", e)
            null
        }
    }

    /**
     * @param customerId Stripe customer id
     * @return Stripe sourceId or null if not set
     */
    override fun getDefaultSource(customerId: String): String? {
        return try {
            Customer.retrieve(customerId).defaultSource
        } catch (e: Exception) {
            LOG.warn("Failed to get default source for customer ${customerId}", e)
            null
        }
    }
}