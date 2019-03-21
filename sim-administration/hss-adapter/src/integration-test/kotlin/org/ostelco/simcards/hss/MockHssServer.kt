package org.ostelco.simcards.hss

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


/**
 * An simple HTTP-serving mock for serving HSS requests. Intended only for
 * test use.
 */
class MockHssServer : Application<MockHssServerConfiguration>() {


    override fun getName(): String {
        return "Mock Hss Server"
    }

    override fun initialize(bootstrap: Bootstrap<MockHssServerConfiguration>?) {
        // nothing to do yet
    }

    private  lateinit var resource: MockHssResource

    override fun run(configuration: MockHssServerConfiguration,
                     env: Environment) {

        this.resource = MockHssResource()
        env.jersey().register(resource);
    }

    fun reset() {
       this.resource.reset()
    }

    fun isActivated(iccid: String): Boolean {
        return this.resource.isActivated(iccid)
    }
}


@JsonInclude(JsonInclude.Include.NON_NULL)
data class Subscription(
        @JsonProperty("bssid")  var  bssid: String,
        @JsonProperty("iccid")  var iccid: String,
        @JsonProperty("msisdn") var msisdn: String,
        @JsonProperty("userid") var userid: String)


/**
 * A very interface that could be used  to connect to an HSS
 */
@Path("/default/provision")
@Produces(MediaType.APPLICATION_JSON)
class MockHssResource() {

    val activated = mutableMapOf<String, Subscription>()

    @POST
    @Timed
    @Path("/activate")
    fun activate(sub: Subscription) : Response {

        activated[sub.iccid] = sub

        return Response.status(Response.Status.CREATED)
                .type(MediaType.APPLICATION_JSON)
                .build()
    }

    @DELETE
    @Timed
    @Path("/deactivate/{iccid}")
    fun deactivate(@PathParam("iccid") iccid:String ) : Response {
        activated.remove(iccid)
        return Response.status(Response.Status.OK)
                .type(MediaType.APPLICATION_JSON)
                .build()
    }

    fun reset() {
        activated.clear()
    }

    fun isActivated(iccid: String): Boolean {
        return activated.contains(iccid)
    }
}


class MockHssServerConfiguration : Configuration() {
}