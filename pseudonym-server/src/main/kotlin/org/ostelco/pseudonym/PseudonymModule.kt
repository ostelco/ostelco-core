package org.ostelco.pseudonym

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.ostelco.prime.module.PrimeModule
import org.ostelco.pseudonym.resources.PseudonymResource
import org.ostelco.pseudonym.service.PseudonymizerServiceSingleton
import java.util.*
import javax.servlet.DispatcherType

@JsonTypeName("pseudonymizer")
class PseudonymModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: PseudonymServerConfig) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        // Allow CORS
        val corsFilterRegistration = env.servlets().addFilter("CORS", CrossOriginFilter::class.java)
        // Configure CORS parameters
        corsFilterRegistration.setInitParameter("allowedOrigins", "*")
        corsFilterRegistration.setInitParameter("allowedHeaders",
                "Cache-Control,If-Modified-Since,Pragma,Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin")
        corsFilterRegistration.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD")
        corsFilterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/*")

        PseudonymizerServiceSingleton.init(env = env)
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
    var namespace = ""
}