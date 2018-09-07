package org.ostelco.at.common

import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.Source
import com.stripe.model.Token

object StripePayment {

    fun createPaymentTokenId(): String {

        // https://stripe.com/docs/api/java#create_card_token
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val cardMap = mapOf(
                "number" to "4242424242424242",
                "exp_month" to 8,
                "exp_year" to 2019,
                "cvc" to "314")

        val tokenMap = mapOf("card" to cardMap)
        val token = Token.create(tokenMap)
        return token.id
    }

    fun createPaymentSourceId(): String {

        // https://stripe.com/docs/api/java#create_source
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        // TODO martin: set valid map values
        val sourceMap = mapOf<String,Any>()
        val source = Source.create(sourceMap)
        return source.id
    }

    fun getCardIdForTokenId(tokenId: String) : String {

        // https://stripe.com/docs/api/java#create_source
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val token = Token.retrieve(tokenId)
        return token.card.id
    }

    fun getDefaultSourceForCustomer(customerId: String) : String {

        // https://stripe.com/docs/api/java#create_source
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val customer = Customer.retrieve(customerId)
        return customer.defaultSource
    }

    fun getCustomerIdForEmail(email: String) : String {

        // https://stripe.com/docs/api/java#create_card_token
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val customers = Customer.list(emptyMap()).data

        return customers.filter { it.email.equals(email) }.first().id
    }

    fun deleteAllCustomers() {
        // https://stripe.com/docs/api/java#create_card_token
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        do {
            val customers = Customer.list(emptyMap()).data
            customers.forEach { customer ->
                customer.delete()
            }
        } while (customers.isNotEmpty())
    }
}