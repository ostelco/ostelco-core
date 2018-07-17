package org.ostelco.prime.paymentprocessor

interface PaymentProcessor {

    enum class Interval(val value: String) {
        DAY("day"),
        WEEK("week"),
        MONTH("month"),
        YEAR("year")
    }

    fun addSource(customerId: String, sourceId: String): String?

    fun createPaymentProfile(userEmail: String): String?

    fun createPlan(productId: String, amount: Int, currency: String, interval: Interval): String?

    fun createProduct(sku: String): String?

    fun getSavedSources(customerId: String): List<String>

    fun getDefaultSource(customerId: String): String?

    fun setDefaultSource(customerId: String, sourceId: String): String?

}