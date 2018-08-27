package org.ostelco.pseudonym

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule
import org.ostelco.pseudonym.resources.PseudonymResource
import org.ostelco.pseudonym.service.PseudonymizerServiceSingleton

@JsonTypeName("pseudonymizer")
class PseudonymModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: PseudonymServerConfig) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        PseudonymizerServiceSingleton.init()
        env.jersey().register(PseudonymResource())
    }
}

object ConfigRegistry {
    var config = PseudonymServerConfig()
}

/**
 * The configuration for Pseudonymiser.
 */
class PseudonymServerConfig : Configuration() {
    var datastoreType = "default"
}