package org.ostelco.simcards.hss

import com.codahale.metrics.annotation.Timed
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import javax.ws.rs.DELETE
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


/**
 * An simple HTTP-serving mock for serving HSS requests. Intended only for
 * test use.
 */
class MockHssServer : Application<MockHssServerConfiguration>() {

    public lateinit var dispatcher: DirectHssDispatcher

    override fun getName(): String {
        return "HSS adapter service"
    }

    override fun initialize(bootstrap: Bootstrap<MockHssServerConfiguration>?) {
        // nothing to do yet
    }

    override fun run(configuration: MockHssServerConfiguration,
                     env: Environment) {

        env.jersey().register(MockHssResource());
    }
}


@Path("/provision")
@Produces(MediaType.APPLICATION_JSON)
class MockHssResource() {

    @POST
    @Timed
    @Path("/activate")
    fun activate() {
    }

    @DELETE
    @Timed
    @Path("/deactivate")
    fun deactivate() {
    }
}


class MockHssServerConfiguration : Configuration() {
}