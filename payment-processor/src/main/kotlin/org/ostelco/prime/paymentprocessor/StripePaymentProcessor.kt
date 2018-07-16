package org.ostelco.prime.paymentprocessor

import com.stripe.model.Customer
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
    override fun createProfile(userEmail: String): String? {
        val customerParams = HashMap<String, Any>()
        customerParams.put("email", userEmail)
        return try {
            Customer.create(customerParams)?.id
        } catch (e: Exception) {
            LOG.warn("Failed to create profile", e)
            null
        }
    }
}