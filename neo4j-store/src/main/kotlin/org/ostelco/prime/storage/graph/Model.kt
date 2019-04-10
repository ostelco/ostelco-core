package org.ostelco.prime.storage.graph

import org.ostelco.prime.model.CustomerRegionStatus
import org.ostelco.prime.model.HasId
import org.ostelco.prime.model.KycStatus
import org.ostelco.prime.model.KycType

data class Identity(
        override val id: String,
        val type: String) : HasId

data class Identifies(val provider: String)

data class SubscriptionToBundle(val reservedBytes: Long = 0)

data class PlanSubscription(
        val subscriptionId: String,
        val created: Long,
        val trialEnd: Long)

data class CustomerRegion(
        val status: CustomerRegionStatus,
        val kycStatusMap: Map<KycType, KycStatus> = emptyMap())
