package org.ostelco.prime.paymentprocessor

class StripePaymentProcessor : PaymentProcessor {
    override fun listSources(stripeUser: String): List<String> {
        return emptyList()
    }
}