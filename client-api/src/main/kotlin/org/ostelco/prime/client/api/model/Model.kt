package org.ostelco.prime.client.api.model

import org.ostelco.prime.model.PurchaseRecord

data class Consent(
        var consentId: String? = null,
        var description: String? = null,
        var accepted: Boolean = false)

data class ConsentList(val consents: List<Consent>)

//data class Grant(
//        val grantType: String,
//        val code: String,
//        val refreshToken: String)

data class SubscriptionStatus(
        var remaining: Long = 0,
        var purchaseRecords: List<PurchaseRecord> = emptyList())
