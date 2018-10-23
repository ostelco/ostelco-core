package org.ostelco.prime.graphql

data class GraphQLRequest(
        val query: String,
        val operationName: String? = null,
        val variables: Map<String, Any> = emptyMap())