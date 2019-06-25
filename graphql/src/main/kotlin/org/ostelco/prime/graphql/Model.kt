package org.ostelco.prime.graphql

import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.RegionDetails
import org.ostelco.prime.model.Subscription

data class GraphQLRequest(
        val query: String,
        val operationName: String? = null,
        val variables: Map<String, Any>? = emptyMap())

data class Context(
        val customer: Customer? = null,
        val bundles: Collection<Bundle>? = null,
        val regions: Collection<RegionDetails>? = null,
        val subscriptions: Collection<Subscription>? = null,
        val products: Collection<Product>? = null,
        val purchases: Collection<PurchaseRecord>? = null)

data class Data(var context: Context? = null)

data class GraphQlResponse(var data: Data? = null, var errors: List<String>? = null)