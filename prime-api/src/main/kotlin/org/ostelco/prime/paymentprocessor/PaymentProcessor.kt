package org.ostelco.prime.paymentprocessor

interface PaymentProcessor {
    fun getSavedSources(paymentId: String): List<String>

    fun createProfile(userEmail: String): String?
}