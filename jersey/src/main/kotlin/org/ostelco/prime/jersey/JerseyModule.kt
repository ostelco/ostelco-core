package org.ostelco.prime.jersey

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("jersey")
class JerseyModule : PrimeModule {

    override fun init(env: Environment) {

        env.jersey().register(YamlMessageBodyReader::class.java)

        /* Add filters/interceptors. */
        env.jersey().register(TrackRequestsLoggingFilter())
    }
}