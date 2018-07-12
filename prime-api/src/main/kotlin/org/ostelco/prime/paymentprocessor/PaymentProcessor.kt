package org.ostelco.prime.paymentprocessor

interface PaymentProcessor {
    fun listSources(stripeUser: String): List<String>
}