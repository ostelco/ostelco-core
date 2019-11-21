package org.ostelco.prime.storage.graph.model

import org.ostelco.prime.model.CustomerRegionStatus
import org.ostelco.prime.model.HasId
import org.ostelco.prime.model.KycStatus
import org.ostelco.prime.model.KycType

data class Identity(
        override val id: String,
        val type: String) : HasId {

    companion object
}

data class Identifies(val provider: String)

data class SubscriptionToBundle(val reservedBytes: Long = 0)

data class PlanSubscription(
        val subscriptionId: String,
        val created: Long,
        val trialEnd: Long)

data class CustomerRegion(
        val status: CustomerRegionStatus,
        val kycStatusMap: Map<KycType, KycStatus> = emptyMap(),
        val kycExpiryDateMap: Map<KycType, String> = emptyMap())

data class SimProfile(
        override val id: String,
        val iccId: String,
        val alias: String = "",
        val requestedOn: String? = null,
        val downloadedOn: String? = null,
        val installedOn: String? = null,
        val deletedOn: String? = null) : HasId {

    companion object
}

data class Segment(override val id: String) : HasId {
    companion object
}

data class Offer(override val id: String) : HasId

data class ExCustomer(
        override val id:String,
        val createdOn: String? = null,
        val terminationDate: String) : HasId {

    companion object
}
