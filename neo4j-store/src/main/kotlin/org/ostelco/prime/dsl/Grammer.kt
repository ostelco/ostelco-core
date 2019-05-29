package org.ostelco.prime.dsl

import org.ostelco.prime.storage.graph.Relation.LINKED_TO_REGION

fun plan(name: String) = PlanContext(name = name)
fun region(regionCode: String) = RegionContext(regionCode = regionCode)

data class RegionContext(val regionCode: String)

class PlanContext(private val name: String) {

    infix fun isLinkedToRegion(region: RegionContext) = RelationContext(
            fromId = name,
            relation = LINKED_TO_REGION,
            toId = region.regionCode)
}