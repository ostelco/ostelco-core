package org.ostelco

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

class Es2plusApplication : Application<Es2plusConfiguration>() {

    override fun getName(): String {
        return "es2+ application"
    }

    override fun initialize(bootstrap: Bootstrap<Es2plusConfiguration>) {
        // TODO: application initialization
    }

    override fun run(configuration: Es2plusConfiguration,
            environment: Environment) {
        // TODO: implement application
        environment.jersey().register(Es2PlusResource())
    }

    companion object {

        @Throws(Exception::class)
        fun main(args: Array<String>) {
            Es2plusApplication().run("foo")
        }
    }

    // We're basing this implementaiton on
    // https://www.gsma.com/newsroom/wp-content/uploads/SGP.22-v2.0.pdf

}

data class Es2PlusDownloadOrder(
        @JsonProperty("eid") val eid: String?,
        @JsonProperty("iccid") val iccid: String?,
        @JsonProperty("profileType") val profileType: String?
)

data class Es2ConfirmOrder(
        @JsonProperty("eid") val eid: String,
        @JsonProperty("iccid") val iccid: String?,
        @JsonProperty("matchingId") val matchingId: String?,
        @JsonProperty("confirmationCode") val confirmationCode: String?,
        @JsonProperty("smdsAddress") val smdsAddress: String?,
        @JsonProperty("releaseFlag") val releaseFlag: String?
)



@Path("/gsma/rsp2/es2plus/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class Es2PlusResource() {


    @Path("downloadOrder")
    @POST
    fun downloadOrder(order: Es2PlusDownloadOrder): Response {
        return Response.created(UriBuilder.fromPath("http://bananas.org/").build()).build()
    }
    
    @Path("confirmOrder")
    @POST
    fun confirmOrder(order: Es2ConfirmOrder): Response {
        return Response.created(UriBuilder.fromPath("http://bananas.org/").build()).build()
    }

    @Path("cancelOrder")
    @POST
    fun cancelOrder(): Response {
        return Response.created(UriBuilder.fromPath("http://bananas.org/").build()).build()
    }

    @Path("releaseProfile")
    @POST
    fun releaseProfile(): Response {
        return Response.created(UriBuilder.fromPath("http://bananas.org/").build()).build()
    }

    @Path("handleDownloadProgressInfo")
    @POST
    fun handleDownloadProgressInfo(): Response {
        return Response.created(UriBuilder.fromPath("http://bananas.org/").build()).build()
    }
}
