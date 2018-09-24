package org.ostelco.prime.paymentprocessor

import com.fasterxml.jackson.annotation.JsonTypeName
import com.stripe.Stripe
import io.dropwizard.setup.Environment
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("stripe-payment-processor")
class PaymentProcessorModule : PrimeModule {

    private val logger by getLogger()

    override fun init(env: Environment) {
        logger.info("PaymentProcessor init with $env")
        Stripe.apiKey = System.getenv("STRIPE_API_KEY") ?: throw Error("Missing environment variable STRIPE_API_KEY")
    }
}
