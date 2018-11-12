package org.ostelco

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import java.io.IOException
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
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


///
///   The fields that all requests needs to have in their headers
///   (for reasons that are unclear to me)
///

data class ES2RequestHeader(
        @JsonProperty("functionRequesterIdentifier") val functionRequesterIdentifier: String,
        @JsonProperty("functionCallIdentifier") val functionCallIdentifier: String
)

///
///   The fields all responses needs to have in their headers
///   (also unknown to me :)
///


data class ES2ResponseHeader(
        @JsonProperty("functionExecutionStatus") val functionExecutionStatus: FunctionExecutionStatus)


enum class FunctionExecutionStatusType {
    @JsonProperty("Executed-Success")
    ExecutedSuccess,
    @JsonProperty("Executed-WithWarning")
    ExecutedWithWarning,
    @JsonProperty("Failed")
    Failed,
    @JsonProperty("Expired")
    Expired
}

data class FunctionExecutionStatus(
        @JsonProperty("status") val status: FunctionExecutionStatusType,
        @JsonProperty("statusCodeData") val statusCodeData: StatusCodeData? = null)

data class StatusCodeData(
        @JsonProperty("subjectCode") var subjectCode: String,
        @JsonProperty("reasonCode") var reasonCode: String,
        @JsonProperty("subjectIdentifier") var subjectIdentifier: String?,
        @JsonProperty("message") var message: String?)


///
///  The DownloadOrder function
///

@JsonSchema("ES2+DownloadOrder-def")
data class Es2PlusDownloadOrder(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("body") val body: Es2PlusDownloadOrderBody
)


data class Es2PlusDownloadOrderBody(
        @JsonProperty("eid") val eid: String?,
        @JsonProperty("iccid") val iccid: String?,
        @JsonProperty("profileType") val profileType: String?
)

@JsonSchema("ES2+DownloadOrder-response")
data class Es2DownloadOrderResponse(
        @JsonProperty("header") val header: ES2ResponseHeader,
        @JsonProperty("body") val body: Es2PlusDownloadOrderResponseBody
)

data class Es2PlusDownloadOrderResponseBody(
        @JsonProperty("iccid") val iccid: String
)

///
/// The ConfirmOrder function
///

@JsonSchema("ES2+ConfirmOrder-def")
data class Es2ConfirmOrder(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("body") val body: Es2ConfirmOrderBody
)

data class Es2ConfirmOrderBody(
        @JsonProperty("eid") val eid: String,
        @JsonProperty("iccid") val iccid: String,
        @JsonProperty("matchingId") val matchingId: String?,
        @JsonProperty("confirmationCode") val confirmationCode: String?,
        @JsonProperty("smdsAddress") val smdsAddress: String?,
        @JsonProperty("releaseFlag") val releaseFlag: Boolean
)

@JsonSchema("ES2+ConfirmOrder-response")
data class Es2ConfirmOrderResponse(
        @JsonProperty("header") val header: ES2ResponseHeader,
        @JsonProperty("body") val body: Es2ConfirmOrderResponseBody)

data class Es2ConfirmOrderResponseBody(
        @JsonProperty("eid") val eid: String,
        @JsonProperty("matchingId") val matchingId: String?,
        @JsonProperty("smdsAddress") val smdsAddress: String?
)

///
///  The CancelOrder function
///

@JsonSchema("ES2+CancelOrder-def")
data class Es2CancelOrder(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("body") val body: Es2CancelOrderBody
)


data class Es2CancelOrderBody(
        @JsonProperty("eid") val eid: String,
        @JsonProperty("iccid") val iccid: String?,
        @JsonProperty("matchingId") val matchingId: String?,
        @JsonProperty("finalProfileStatusIndicator") val finalProfileStatusIndicator: String?
)

@JsonSchema("ES2+CancelOrder-response")
data class Es2CancelOrderResponse(@JsonProperty("header") val header: ES2ResponseHeader)


///
///  The ReleaseProfile function
///

@JsonSchema("ES2+ReleaseProfile-def")
data class Es2ReleaseProfile(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("body") val body: Es2ReleaseProfileBody
)


data class Es2ReleaseProfileBody(
        @JsonProperty("iccid") val iccid: String
)

@JsonSchema("ES2+ReleaseProfile-response")
data class Es2ReleaseProfileResponse(
        @JsonProperty("header") val header: ES2ResponseHeader)


///
///  The The HandleDownloadProgressInfo function
///

@JsonSchema("ES2+HandleDownloadProgressInfo-def")
data class Es2HandleDownloadProgressInfo(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("body") val body: Es2HandleDownloadProgressInfoBody
)

data class Es2HandleDownloadProgressInfoBody(
        @JsonProperty("eid") val eid: String? = null,
        @JsonProperty("iccid") val iccid: String? = null,
        @JsonProperty("profileType") val profileType: String? = null,
        @JsonProperty("timestamp") val timestamp: String? = null,
        @JsonProperty("notificationPointId") val notificationPointId: String? = null,
        @JsonProperty("notificationPointStatus") val notificationPointStatus: ES2NotificationPointStatus? = null,
        @JsonProperty("resultData") val resultData: ES2StatusCodeData? = null
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

@JsonSchema("ES2+HandleDownloadProgressInfo-response")
data class Es2HandleDownloadProgressInfoResponse(
        @JsonProperty("header") val header: ES2ResponseHeader)


///
///  The web resource using the protocol domain model.
///

@Path("/gsma/rsp2/es2plus/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class Es2PlusResource() {

    @Path("downloadOrder")
    @POST
    fun downloadOrder(order: Es2PlusDownloadOrder): Es2DownloadOrderResponse {
        val iccid = if (order.body.iccid != null) order.body.iccid else "01234567890123456798"
        val response =
                Es2DownloadOrderResponse(
                        header = ES2ResponseHeader(functionExecutionStatus = FunctionExecutionStatus(
                                status = FunctionExecutionStatusType.ExecutedSuccess,
                                statusCodeData = StatusCodeData(
                                        subjectCode = "foo",
                                        reasonCode = "bar",
                                        subjectIdentifier = "baz",
                                        message = "gazonk"))),
                        body = Es2PlusDownloadOrderResponseBody(iccid))
        return response
    }


    @Path("confirmOrder")
    @POST
    fun confirmOrder(order: Es2ConfirmOrder): Es2ConfirmOrderResponse {
        return Es2ConfirmOrderResponse(
                header = ES2ResponseHeader(functionExecutionStatus = FunctionExecutionStatus(
                        status = FunctionExecutionStatusType.ExecutedSuccess,
                        statusCodeData = StatusCodeData(
                                subjectCode = "foo",
                                reasonCode = "bar",
                                subjectIdentifier = "baz",
                                message = "gazonk"))),
                body = Es2ConfirmOrderResponseBody(
                        eid = order.body.eid,
                        smdsAddress = order.body.smdsAddress,
                        matchingId = order.body.matchingId))
    }

    @Path("cancelOrder")
    @POST
    fun cancelOrder(order: Es2CancelOrder): Es2CancelOrderResponse {
        return Es2CancelOrderResponse(
                header = ES2ResponseHeader(functionExecutionStatus = FunctionExecutionStatus(
                        status = FunctionExecutionStatusType.ExecutedSuccess,
                        statusCodeData = StatusCodeData(
                                subjectCode = "foo",
                                reasonCode = "bar",
                                subjectIdentifier = "baz",
                                message = "gazonk"))))
    }

    @Path("releaseProfile")
    @POST
    fun releaseProfile(order: Es2ReleaseProfile): Es2ReleaseProfileResponse {
        return Es2ReleaseProfileResponse(
                header = ES2ResponseHeader(
                        functionExecutionStatus = FunctionExecutionStatus(
                                status = FunctionExecutionStatusType.ExecutedSuccess,
                                statusCodeData = StatusCodeData(
                                        subjectCode = "foo",
                                        reasonCode = "bar",
                                        subjectIdentifier = "baz",
                                        message = "gazonk"))))
    }

    @Path("handleDownloadProgressInfo")
    @POST
    fun handleDownloadProgressInfo(order: Es2HandleDownloadProgressInfo): Es2HandleDownloadProgressInfoResponse {
        return Es2HandleDownloadProgressInfoResponse(
                header = ES2ResponseHeader(
                        functionExecutionStatus = FunctionExecutionStatus(
                                status = FunctionExecutionStatusType.ExecutedSuccess,
                                statusCodeData = StatusCodeData(
                                        subjectCode = "foo",
                                        reasonCode = "bar",
                                        subjectIdentifier = "baz",
                                        message = "gazonk"))))
    }
}
