package org.ostelco.prime.graphql

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Path("/graphql")
class GraphQLResource(private val queryHandler: QueryHandler) {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun handlePost(request: GraphQLRequest): Any = executeOperation(request)

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun handleGet(
            @QueryParam("query") query: String,
            @QueryParam("variables") variables: Map<String, Any>): Any = executeOperation(GraphQLRequest(query = query, variables = variables))

    private fun executeOperation(request: GraphQLRequest): Any {
        val executionResult = queryHandler.execute(query = request.query, variables = request.variables)
        val result = mutableMapOf<String, Any>()
        if (executionResult.errors.isNotEmpty()) {
            result["errors"] = executionResult.errors
        }
        val data: Any? = executionResult.getData()
        if (data != null) {
            result["data"] = data
        }
        return result
    }
}