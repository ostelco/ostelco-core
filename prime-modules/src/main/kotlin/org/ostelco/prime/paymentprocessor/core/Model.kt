package org.ostelco.prime.paymentprocessor.core

/* Intended for reporting status for payment operations. */
enum class PaymentStatus {
    PAYMENT_SUCCEEDED,
    TRIAL_START,
    REQUIRES_PAYMENT_METHOD,
    REQUIRES_ACTION,
}

data class PlanInfo(val id: String)

data class ProductInfo(val id: String)

data class ProfileInfo(val id: String)

data class SourceInfo(val id: String)

data class SourceDetailsInfo(val id: String, val type: String, val details: Map<String, Any>)

data class SubscriptionInfo(val id: String)

data class SubscriptionDetailsInfo(val id: String, val status: PaymentStatus, val invoiceId: String, val created: Long, val trialEnd: Long = 0L)
