package org.ostelco

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import java.io.IOException
import java.util.stream.Collectors
import java.util.stream.Stream
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

        // XXX Add these parameters to configuration file.
        val oas = OpenAPI()
        val info = Info()
                .title(getName())
                .description("Restful membership management.")
                .termsOfService("http://example.com/terms")
                .contact(Contact().email("la3lma@gmail.com"))

        oas.info(info)
        val oasConfig = SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .resourcePackages(Stream.of("no .rmz.membershipmgt")
                        .collect(Collectors.toSet<String>()))
        environment.jersey().register(OpenApiResource()
                .openApiConfiguration(oasConfig))

        environment.jersey().register(Es2PlusResource(SmDpPlus()))
        environment.jersey().register(RestrictedOperationsRequestFilter())
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            Es2plusApplication().run(*args)
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

/*


// Starting point for intercepting exceptions, so that they can
// be wrapped in a return value of sorts.

class Es2Exception extends Exception {
}


class AppExceptionMapper : ExceptionMapper<Es2Exception> {
    fun toResponse(ex: Es2Exception): Response {
        return Response.status(ex.getStatus())
                .entity(ErrorMessage(ex))
                .type(MediaType.APPLICATION_JSON).build()
    }
}

*/
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



class SmDpPlus {

    // XXX The ICCID generated should be  unique, not yet allocated, etc.
    fun downloadOrder(eid: String?, iccid: String?, profileType: String?): String {
        val iccid = if (iccid != null) iccid else "01234567890123456798"
        return iccid
    }

    // XXX Throw exception if order can't be confirmed, also: Are all these parameters
    //     needed?
    fun confirmOrder(eid: String, smdsAddress: String?, machingId: String?, confirmationCode: String?) {

    }

}

///
///  The web resource using the protocol domain model.
///

@Path("/gsma/rsp2/es2plus/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class Es2PlusResource(val smDpPlus: SmDpPlus) {

    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
    @Path("downloadOrder")
    @POST
    fun downloadOrder(order: Es2PlusDownloadOrder): Es2DownloadOrderResponse {

        val iccid = smDpPlus.downloadOrder(
                eid = order.body.eid,
                iccid = order.body.iccid,
                profileType = order.body.profileType)

        return Es2DownloadOrderResponse(
                header = ES2ResponseHeader(
                        functionExecutionStatus = FunctionExecutionStatus(
                                status = FunctionExecutionStatusType.ExecutedSuccess,
                                statusCodeData = StatusCodeData(
                                        subjectCode = "foo",  // XXX WTF is this
                                        reasonCode = "bar",   // .... and this
                                        subjectIdentifier = "baz", //  and this?  GSMA isn't particulary clear
                                        message = "gazonk"))),
                body = Es2PlusDownloadOrderResponseBody(iccid))
    }


    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
    @Path("confirmOrder")
    @POST
    fun confirmOrder(order: Es2ConfirmOrder): Es2ConfirmOrderResponse {

        smDpPlus.confirmOrder(eid=order.body.eid,
                smdsAddress=order.body.smdsAddress,
                machingId=order.body.matchingId,
                confirmationCode = order.body.confirmationCode)

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

    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
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

    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
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

    /**
     * This method is intended to be called _by_ the SM-DP+, sending information
     * back to the  operator's BSS system about the progress of various
     * operations.
     */
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
