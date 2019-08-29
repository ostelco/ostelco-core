package org.ostelco.prime.tracing

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("tracing")
class TracingModule : PrimeModule {

    override fun init(env: Environment) {
        TraceSingleton.init()
        env.jersey().register(TracingFeature::class.java)
    }
}