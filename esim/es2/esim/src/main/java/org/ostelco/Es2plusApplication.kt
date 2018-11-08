package org.ostelco

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import javax.ws.rs.*
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.ext.Provider


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
        environment.jersey().register(RestrictedOperationsRequestFilter())
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



@Provider
class RestrictedOperationsRequestFilter : ContainerRequestFilter {

    @Throws(IOException::class)
    override fun filter(ctx: ContainerRequestContext) {
        val adminProtocol = ctx.headers.getFirst("X-Admin-Protocol")
        val userAgent = ctx.headers.getFirst("User-Agent")

        if (!"gsma-rsp-lpad".equals(userAgent)) {
            ctx.abortWith(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Illegal user agent, expected gsma-rsp-lpad")
                    .build())
        } else if (adminProtocol == null || !adminProtocol!!.startsWith("gsma/rsp/")) {
            ctx.abortWith(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Illegal X-Admin-Protocol header, expected something starting with \"gsma/rsp/\"")
                    .build())
        }
    }
}


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class JsonSchema(val schemaKey: String)

@JsonSchema("ES2+DownloadOrder-def")
data class Es2PlusDownloadOrder(
        @JsonProperty("eid") val eid: String?,
        @JsonProperty("iccid") val iccid: String?,
        @JsonProperty("profileType") val profileType: String?
)

@JsonSchema("ES2+DownloadOrder-response")
data class Es2DownloadOrderResponse( @JsonProperty("iccid") val iccid: String)

data class Es2ConfirmOrder(
        @JsonProperty("eid") val eid: String,
        @JsonProperty("iccid") val iccid: String,
        @JsonProperty("matchingId") val matchingId: String?,
        @JsonProperty("confirmationCode") val confirmationCode: String?,
        @JsonProperty("smdsAddress") val smdsAddress: String?,
        @JsonProperty("releaseFlag") val releaseFlag: Boolean
)

@JsonSchema("ES2+ConfirmOrder-response")
data class Es2ConfirmOrderResponse(
        @JsonProperty("eid") val eid: String,
        @JsonProperty("matchingId") val matchingId: String?,
        @JsonProperty("smdsAddress") val smdsAddress: String?
)


data class Es2CancelOrder(
        @JsonProperty("eid") val eid: String,
        @JsonProperty("iccid") val iccid: String?,
        @JsonProperty("matchingId") val matchingId: String?,
        @JsonProperty("finalProfileStatusIndicator") val confirmationCode: String?
)

data class Es2ReleaseProfile(
        @JsonProperty("iccid") val iccid: String
)

data class ES2NotificationPointStatus(
        @JsonProperty("status") val status: String, // "Executed-Success, Executed-WithWarning, Failed or
        @JsonProperty("statusCodeData") val statusCodeData: ES2StatusCodeData?
        )

data class ES2StatusCodeData(
        @JsonProperty("subjectCode") val subjectCode: String, // "Executed-Success, Executed-WithWarning, Failed or
        @JsonProperty("reasonCode") val statusCodeData: String,
        @JsonProperty("subjectIdentifier") val subjectIdentifier: String?,
        @JsonProperty("message") val message: String?
)

data class Es2HandleDownloadProgressInfo(
        @JsonProperty("eid") val eid: String?,
        @JsonProperty("iccid") val iccid: String?,
        @JsonProperty("profileType") val profileType: String?,
        @JsonProperty("timestamp") val timestamp: String?,
        @JsonProperty("notificationPointId") val notificationPointId: String?,
        @JsonProperty("notificationPointStatus") val notificationPointStatus: ES2NotificationPointStatus?,
        @JsonProperty("resultData") val resultData: ES2StatusCodeData?
        )



@Path("/gsma/rsp2/es2plus/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class Es2PlusResource() {


    @Path("downloadOrder")
    @POST
    fun downloadOrder(order: Es2PlusDownloadOrder): Es2DownloadOrderResponse {
        val iccid = if (order.iccid != null) order.iccid else "01234567890123456798"
        val response = Es2DownloadOrderResponse(iccid)
        return response
    }

    @Path("confirmOrder")
    @POST
    fun confirmOrder(order: Es2ConfirmOrder): Response {
        return Response.created(UriBuilder.fromPath("http://bananas.org/").build()).build()
    }

    @Path("cancelOrder")
    @POST
    fun cancelOrder(order: Es2CancelOrder): Response {
        return Response.created(UriBuilder.fromPath("http://bananas.org/").build()).build()
    }

    @Path("releaseProfile")
    @POST
    fun releaseProfile(order: Es2ReleaseProfile): Response {
        return Response.created(UriBuilder.fromPath("http://bananas.org/").build()).build()
    }

    @Path("handleDownloadProgressInfo")
    @POST
    fun handleDownloadProgressInfo(order: Es2HandleDownloadProgressInfo): Response {
        return Response.created(UriBuilder.fromPath("http://bananas.org/").build()).build()
    }
}
