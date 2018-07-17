package org.ostelco.prime.paymentprocessor

interface PaymentProcessor {

    enum class Interval(val value: String) {
        DAY("day"),
        WEEK("week"),
        MONTH("month"),
        YEAR("year")
    }

    fun addSource(customerId: String, sourceId: String): String?

    fun getSavedSources(paymentId: String): List<String>

    fun createPaymentProfile(userEmail: String): String?

    fun createPlan(productId: String, amount: Int, currency: String, interval: Interval): String?

    fun createProduct(sku: String): String?

}