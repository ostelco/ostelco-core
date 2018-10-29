package org.ostelco.prime.graphql

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import java.io.File


class QueryHandler(schemaFile: File) {

    private val graphQL = SchemaGenerator()
            .makeExecutableSchema(
                    SchemaParser().parse(schemaFile),
                    buildRuntimeWiring())
            .let { GraphQL.newGraphQL(it).build() }

    fun execute(subscriberId: String, query: String, operationName: String? = null, variables: Map<String, Any>? = null): ExecutionResult{
        var executionInputBuilder = ExecutionInput.newExecutionInput()
                .query(query)
                .context(subscriberId)
        if (operationName != null) {
            executionInputBuilder = executionInputBuilder.operationName(operationName)
        }
        if (variables != null) {
            executionInputBuilder = executionInputBuilder.variables(variables)
        }
        return graphQL.execute(executionInputBuilder.build())
    }
}