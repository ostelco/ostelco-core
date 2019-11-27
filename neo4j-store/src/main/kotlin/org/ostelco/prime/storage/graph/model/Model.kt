package org.ostelco.prime.storage.graph.model

import org.ostelco.prime.model.CustomerRegionStatus
import org.ostelco.prime.model.HasId
import org.ostelco.prime.model.KycStatus
import org.ostelco.prime.model.KycType

/*
  This are the Neo4jStore's internal entity classes, which are almost similar to their prime model's counterparts.
  These classes have been given the same name of their prime model's counterparts as a convention.
  These classes exists because for some of the prime model's entity classes cannot be stored as it is in Neo4j.
  Some of the reasons why that is done so is:
  1. A single class in prime model gets split into 2 classes in Graph DB - one for entity and one for relation.
     E.g. [Identity] and [Identifies].
  2. A single class in prime model is composition for multiple entities in Graph DB.
     E.g. [Segment] and [Offer].
  3. Graph DB has partial information compared to prime model's class.
     E.g. [SimProfile] in Graph DB does not have SimStatus and ActivationCode, which is stored in Sim Manager.
*/

data class Identity(
        override val id: String,
        val type: String) : HasId {

    companion object
}

data class Identifies(val provider: String)

data class SubscriptionToBundle(
        val reservedBytes: Long = 0,
        val reservedOn: String? = null
)

data class PlanSubscription(
        val subscriptionId: String,
        val created: Long,
        val trialEnd: Long)

data class CustomerRegion(
        val status: CustomerRegionStatus,
        val kycStatusMap: Map<KycType, KycStatus> = emptyMap(),
        val kycExpiryDateMap: Map<KycType, String> = emptyMap(),
        val initiatedOn: String? = null,
        val approvedOn: String? = null)

data class SimProfile(
        override val id: String,
        val iccId: String,
        val alias: String = "",
        val requestedOn: String? = null,
        val downloadedOn: String? = null,
        val installedOn: String? = null,
        val installedReportedByAppOn: String? = null,
        val deletedOn: String? = null) : HasId {

    companion object
}

data class Segment(override val id: String) : HasId {
    companion object
}

data class Offer(override val id: String) : HasId {

    companion object
}

data class ExCustomer(
        override val id:String,
        val createdOn: String? = null,
        val terminationDate: String) : HasId {

    companion object
}
