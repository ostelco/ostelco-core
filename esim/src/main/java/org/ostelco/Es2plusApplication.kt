package org.ostelco

import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.ostelco.es2plus.*
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
                .title(name)
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

    // We're basing this implementation on
    // https://www.gsma.com/newsroom/wp-content/uploads/SGP.22-v2.0.pdf
}

@Provider
class RestrictedOperationsRequestFilter : ContainerRequestFilter {

    @Throws(IOException::class)
    override fun filter(ctx: ContainerRequestContext) {
        val adminProtocol = ctx.headers.getFirst("X-Admin-Protocol")
        val userAgent = ctx.headers.getFirst("User-Agent")

        if ("gsma-rsp-lpad" != userAgent) {
            ctx.abortWith(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Illegal user agent, expected gsma-rsp-lpad")
                    .build())
        } else if (adminProtocol == null || !adminProtocol.startsWith("gsma/rsp/")) {
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


class SmDpPlus {

    // XXX The ICCID generated should be  unique, not yet allocated, etc.
    fun downloadOrder(eid: String?, iccid: String?, profileType: String?): String {
        return iccid ?: "01234567890123456798"
    }

    // XXX Throw exception if order can't be confirmed, also: Are all these parameters
    //     needed?
    fun confirmOrder(eid: String, smdsAddress: String?, machingId: String?, confirmationCode: String?) {

    }

}

///
///  The web resource using the protocol domain model.
///


@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/gsma/rsp2/es2plus/")
class Es2PlusResource(val smDpPlus: SmDpPlus) {

    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
    @Path("downloadOrder")
    @POST
    fun downloadOrder(order: Es2PlusDownloadOrder): Es2DownloadOrderResponse {

        val iccid = smDpPlus.downloadOrder(
                eid = order.eid,
                iccid = order.iccid,
                profileType = order.profileType)

        return Es2DownloadOrderResponse(
                header = ES2ResponseHeader(
                        functionExecutionStatus = FunctionExecutionStatus(
                                status = FunctionExecutionStatusType.ExecutedSuccess,
                                statusCodeData = StatusCodeData(
                                        subjectCode = "foo",  // XXX WTF is this
                                        reasonCode = "bar",   // .... and this
                                        subjectIdentifier = "baz", //  and this?  GSMA isn't particulary clear
                                        message = "gazonk"))),
                iccid = iccid)
    }


    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
    @Path("confirmOrder")
    @POST
    fun confirmOrder(order: Es2ConfirmOrder): Es2ConfirmOrderResponse {

        smDpPlus.confirmOrder(eid = order.eid,
                smdsAddress = order.smdsAddress,
                machingId = order.matchingId,
                confirmationCode = order.confirmationCode)

        return Es2ConfirmOrderResponse(
                header = ES2ResponseHeader(functionExecutionStatus = FunctionExecutionStatus(
                        status = FunctionExecutionStatusType.ExecutedSuccess,
                        statusCodeData = StatusCodeData(
                                subjectCode = "foo",
                                reasonCode = "bar",
                                subjectIdentifier = "baz",
                                message = "gazonk"))),
                eid = order.eid,
                smdsAddress = order.smdsAddress,
                matchingId = order.matchingId)
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
