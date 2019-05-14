package org.ostelco.prime.graphql

import graphql.schema.idl.RuntimeWiring

fun buildRuntimeWiring(): RuntimeWiring {
    return RuntimeWiring.newRuntimeWiring()
            .type("QueryType") { typeWiring ->
                typeWiring.dataFetcher("context", ContextDataFetcher())
            }
            .build()
}