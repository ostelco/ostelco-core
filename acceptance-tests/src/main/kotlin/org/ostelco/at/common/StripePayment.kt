package org.ostelco.at.common

import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.Source
import com.stripe.model.Token
import com.stripe.model.WebhookEndpoint
import java.time.Year

object StripePayment {

    private val logger by getLogger()

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

    fun createInsuffientFundsPaymentSourceId(): String {

        // https://stripe.com/docs/api/java#create_source
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val sourceMap = mapOf(
                "type" to "card",
                "card" to mapOf(
                        "number" to "4000000000000341",
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

    fun getCardIdForTokenId(tokenId: String) : String {

        // https://stripe.com/docs/api/java#create_source
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val token = Token.retrieve(tokenId)
        return token.card.id
    }

    val MAX_TRIES = 3
    val WAIT_DELAY = 300L

    /**
     * Obtains 'default source' directly from Stripe. Use in tests to
     * verify that the correspondng 'setDefaultSource' API works as
     * intended.
     */
    fun getDefaultSourceForCustomer(stripeCustomerId: String) : String {

        // https://stripe.com/docs/api/java#create_source
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        var error = Exception()

        (0..MAX_TRIES).forEach {
            try {
                return Customer.retrieve(stripeCustomerId).defaultSource
            } catch (e: Exception) {
                error = e
            }
        }

        throw(error)
    }

    /**
     * Obtains the Stripe 'customerId' directly from Stripe.
     */
    fun getStripeCustomerId(customerId: String) : String {

        // https://stripe.com/docs/api/java#create_card_token
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        var customers: List<Customer> = emptyList()

        (0..MAX_TRIES).forEach {
            customers = Customer.list(emptyMap()).data
            if (!customers.isEmpty())
                return@forEach
            Thread.sleep(WAIT_DELAY)
        }

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

    fun enableWebhook(on: Boolean = true) {

        // https://stripe.com/docs/api/java#create_card_token
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")

        val endpoint = WebhookEndpoint.list(emptyMap()).data
                .filter {
                    it.url.contains("serveo")
                }

        if (endpoint.isNotEmpty())
            endpoint.first()
                    .update(mapOf(
                            "disabled" to !on
                    ))
        else
            logger.error("Found no webhook endpoint configured on Stripe using the serveo.net service")
    }

    private fun nextYear() = Year.now().value + 1
}

// use this just for cleanup
fun main() = StripePayment.deleteAllCustomers()
