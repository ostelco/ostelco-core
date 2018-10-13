package org.ostelco.prime.jersey

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("jersey")
class JerseyModule : PrimeModule {

    override fun init(env: Environment) {

        // Read incoming YAML requests
        env.jersey().register(YamlMessageBodyReader::class.java)

        // filter to set TraceID in Logging MDC
        env.jersey().register(TrackRequestsLoggingFilter())

        // ping resource to check connectivity
        env.jersey().register(PingResource())
    }
}