package org.ostelco.prime.paymentprocessor.core

data class PlanInfo(val id: String)

data class ProductInfo(val id: String)

data class ProfileInfo(val id: String)

data class SourceInfo(val id: String)

data class SourceDetailsInfo(val id: String, val type: String, val details: Map<String, Any>)

data class SubscriptionInfo(val id: String, val status: String? = null, val invoiceId: String? = null, val created: Long = 0L, val trialEnd: Long = 0L)
