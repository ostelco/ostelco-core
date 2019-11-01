package org.ostelco.prime.paymentprocessor.core

import java.math.BigDecimal

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

data class SubscriptionDetailsInfo(val id: String, val status: PaymentStatus, val invoiceId: String, val chargeId: String, val created: Long, val trialEnd: Long = 0L)

data class TaxRateInfo(val id: String, val percentage: BigDecimal, val displayName: String, val inclusive: Boolean)

data class InvoiceItemInfo(val id: String)

data class InvoiceInfo(val id: String)

data class InvoicePaymentInfo(val id: String, val chargeId: String)

data class PaymentTransactionInfo(val id: String, val amount: Int, val currency: String, val created: Long, val refunded: Boolean)
