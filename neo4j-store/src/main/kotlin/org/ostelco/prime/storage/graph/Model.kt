package org.ostelco.prime.storage.graph

import org.ostelco.prime.model.CustomerRegionStatus
import org.ostelco.prime.model.HasId

data class Identity(
        override val id: String,
        val type: String) : HasId

data class Identifies(val provider: String)

enum class StatusFlag {
    JUMIO,
    MY_INFO,
    NRIC_FIN,
    ADDRESS_AND_PHONE_NUMBER
}

data class SubscriptionToBundle(val reservedBytes: Long = 0)

data class PlanSubscription(
        val subscriptionId: String,
        val created: Long,
        val trialEnd: Long)

data class CustomerRegion(
        val status: CustomerRegionStatus,
        val bitMapStatusFlags: Int)