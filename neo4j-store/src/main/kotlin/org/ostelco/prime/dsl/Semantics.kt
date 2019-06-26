package org.ostelco.prime.dsl

import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.HasId
import org.ostelco.prime.storage.graph.Neo4jStoreSingleton
import org.ostelco.prime.storage.graph.RelationType
import org.ostelco.prime.storage.graph.model.Identity


data class RelatedClause<FROM : HasId, TO : HasId>(
        val relationType: RelationType<FROM, *, TO>,
        val fromId: String)

data class RelatedFromClause<FROM : HasId, TO : HasId>(
        val relationType: RelationType<FROM, *, TO>,
        val toId: String)

// (Identity) -[IDENTIFIES]-> (Customer)
// (Customer) -[HAS_SUBSCRIPTION]-> (Subscription)
// (Customer) -[HAS_BUNDLE]-> (Bundle)
// (Customer) -[HAS_SIM_PROFILE]-> (SimProfile)
// (Customer) -[SUBSCRIBES_TO_PLAN]-> (Plan)
// (Subscription) -[LINKED_TO_BUNDLE]-> (Bundle)
// (Customer) -[PURCHASED]-> (Product)
// (Customer) -[REFERRED]-> (Customer)
// (Offer) -[OFFERED_TO_SEGMENT]-> (Segment)
// (Offer) -[OFFER_HAS_PRODUCT]-> (Product)
// (Customer) -[BELONG_TO_SEGMENT]-> (Segment)
// (Customer) -[EKYC_SCAN]-> (ScanInformation)
// (Customer) -[BELONG_TO_REGION]-> (Region)
// (SimProfile) -[SIM_PROFILE_FOR_REGION]-> (Region)
// (Subscription) -[SUBSCRIPTION_UNDER_SIM_PROFILE]-> (SimProfile)


data class IdentityContext(val id: String)
data class CustomerContext(val id: String)

//
// Identity
//

infix fun Identity.Companion.withId(id: String) = IdentityContext(id)

infix fun Identity.Companion.whichIdentifies(customer: CustomerContext) =
        RelatedFromClause(
                relationType = Neo4jStoreSingleton.identifiesRelation,
                toId = customer.id
        )

//
// Customer
//

infix fun Customer.Companion.withId(id: String): CustomerContext = CustomerContext(id)

infix fun Customer.Companion.identifiedBy(identity: IdentityContext) =
        RelatedClause(
                relationType = Neo4jStoreSingleton.identifiesRelation,
                fromId = identity.id
        )