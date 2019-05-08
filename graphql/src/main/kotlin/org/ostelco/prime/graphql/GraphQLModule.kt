package org.ostelco.prime.graphql

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule
import java.io.File

@JsonTypeName("graphql")
class GraphQLModule : PrimeModule {

    @JsonProperty
    var config: Config = Config(schemaFile = "/config/customer.graphqls")

    override fun init(env: Environment) {
        env.jersey().register(
                GraphQLResource(QueryHandler(File(config.schemaFile))))
    }
}

data class Config(val schemaFile: String)