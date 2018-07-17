package org.ostelco.prime.paymentprocessor

import com.stripe.model.Customer
import com.stripe.model.Plan
import com.stripe.model.Product
import org.ostelco.prime.logger

class StripePaymentProcessor : PaymentProcessor {

    private val LOG by logger()

    override fun getSavedSources(stripeUser: String): List<String> {
        val customer = Customer.retrieve(stripeUser)
        println("Customer is : ${customer}")
        println("Customer has source : ${customer.sources.data}")
        return emptyList()
    }

    /**
     * Return Stripe customerId or null if not created
     */
    override fun createPaymentProfile(userEmail: String): String? {
        val customerParams = HashMap<String, Any>()
        customerParams.put("email", userEmail)
        return try {
            Customer.create(customerParams)?.id
        } catch (e: Exception) {
            LOG.warn("Failed to create profile", e)
            null
        }
    }

    /**
     * Return Stripe planId or null if not created
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
            LOG.warn("Failed to create plan", e)
            null
        }
    }

    /**
     * Return Stripe productId or null if not created
     */
    override fun createProduct(sku: String): String? {
        val productParams = HashMap<String, Any>()
        productParams["name"] = sku
        productParams["type"] = "service"

        return try {
            Product.create(productParams)?.id
        } catch (e : Exception) {
            LOG.warn("Failed to create Product", e)
            null
        }
    }

    /**
     * Return Stripe sourceId or null if not created
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
}