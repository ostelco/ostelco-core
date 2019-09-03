package org.ostelco.at.common

import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.Source
import com.stripe.model.Token
import java.time.Year

object StripePayment {

    fun createPaymentTokenId(): String {

        // https://stripe.com/docs/api/java#create_card_token
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val cardMap = mapOf(
                "number" to "4242424242424242",
                "exp_month" to 12,
                "exp_year" to nextYear(),
                "cvc" to "314")

        val tokenMap = mapOf("card" to cardMap)
        val token = Token.create(tokenMap)
        return token.id
    }

    fun createPaymentSourceId(): String {

        // https://stripe.com/docs/api/java#create_source
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val sourceMap = mapOf(
                "type" to "card",
                "card" to mapOf(
                        "number" to "4242424242424242",
                        "exp_month" to 12,
                        "exp_year" to nextYear(),
                        "cvc" to "314"),
                "owner" to mapOf(
                        "address" to mapOf(
                                "city" to "Oslo",
                                "country" to "Norway"
                        ),
                        "email" to "me@somewhere.com")
                )
        val source = Source.create(sourceMap)
        return source.id
    }

    fun createPaymentSourceIdNoAddress(): String {

        // https://stripe.com/docs/api/java#create_source
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val sourceMap = mapOf(
                "type" to "card",
                "card" to mapOf(
                        "number" to "4242424242424242",
                        "exp_month" to 12,
                        "exp_year" to nextYear(),
                        "cvc" to "314"),
                "owner" to mapOf(
                        "email" to "me@somewhere.com")
        )
        val source = Source.create(sourceMap)
        return source.id
    }

    fun getCardIdForTokenId(tokenId: String) : String {

        // https://stripe.com/docs/api/java#create_source
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val token = Token.retrieve(tokenId)
        return token.card.id
    }

    /**
     * Obtains 'default source' directly from Stripe. Use in tests to
     * verify that the correspondng 'setDefaultSource' API works as
     * intended.
     */
    fun getDefaultSourceForCustomer(stripeCustomerId: String) : String {

        // https://stripe.com/docs/api/java#create_source
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val customer = Customer.retrieve(stripeCustomerId)
        return customer.defaultSource
    }

    /**
     * Obtains the Stripe 'customerId' directly from Stripe.
     */
    fun getStripeCustomerId(customerId: String) : String {
        // https://stripe.com/docs/api/java#create_card_token
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val customers = Customer.list(emptyMap()).data
        return customers.first { it.id == customerId }.id
    }

    fun deleteCustomer(customerId: String) {
        // https://stripe.com/docs/api/java#create_card_token
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")
        val customers = Customer.list(emptyMap()).data
        customers.filter { it.id == customerId }
                .forEach { it.delete() }
    }

    fun deleteAllCustomers() {
        // https://stripe.com/docs/api/java#create_card_token
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")
        while (true) {
            val customers = Customer.list(emptyMap()).data
            if (customers.isEmpty()) {
                break
            }
            customers.forEach {
                        println(it.email)
                        it.delete()
                    }
        }
    }

    private fun nextYear() = Year.now().value + 1
}

// use this just for cleanup
fun main() = StripePayment.deleteAllCustomers()
