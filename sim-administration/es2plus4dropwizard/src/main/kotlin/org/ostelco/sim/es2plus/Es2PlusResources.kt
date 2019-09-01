package org.ostelco.sim.es2plus

import io.dropwizard.jersey.setup.JerseyEnvironment
import org.ostelco.jsonschema.DynamicES2ValidatorAdder
import org.ostelco.prime.jersey.logging.Critical
import org.ostelco.sim.es2plus.ES2PlusClient.Companion.X_ADMIN_PROTOCOL_HEADER_VALUE
import org.ostelco.sim.es2plus.SmDpPlusServerResource.Companion.ES2PLUS_PATH_PREFIX
import org.slf4j.LoggerFactory
import java.io.IOException
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider


@Provider
class ES2PlusIncomingHeadersFilter : ContainerRequestFilter {

    private val logger = LoggerFactory.getLogger(ES2PlusIncomingHeadersFilter::class.java)

    companion object {
        fun addEs2PlusDefaultFiltersAndInterceptors(env: JerseyEnvironment) {

            // XXX Replace these with dynamic adders
            env.register(ES2PlusIncomingHeadersFilter())
            env.register(ES2PlusOutgoingHeadersFilter())
            env.register(SmdpExceptionMapper())

            // Like this one...
            env.register(DynamicES2ValidatorAdder())
        }
    }

    @Throws(IOException::class)
    override fun filter(ctx: ContainerRequestContext) {

        val uri = ctx.uriInfo.path
        if (!uri.startsWith(ES2PLUS_PATH_PREFIX)) {
            return
        }

        val adminProtocol: String? = ctx.headers.getFirst("X-Admin-Protocol")
        val userAgent: String? = ctx.headers.getFirst("User-Agent")


        if (!listOf("gsma-rsp-lpad", "gsma-rsp-lpae").contains(userAgent)) {
            logger.warn("Illegal user agent, expected: 'gsma-rsp-lpad' or 'gsma-rsp-lpae', actual: '{}'", userAgent)
            // TODO rmz: Add configuration to make strict mode configurable
        }

        // This looks weird, but it's also excluding null values in a "boolean" check,
        // so it's actually legit :-)
        if (adminProtocol?.startsWith("gsma/rsp/") != true) {
            logger.warn("Illegal X-Admin-Protocol header: {}, expected something starting with 'gsma/rsp/'", adminProtocol)
            // TODO rmz: Add configuration to make strict mode configurable
        }
    }
}

@Provider
class ES2PlusOutgoingHeadersFilter : ContainerResponseFilter {

    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext,
                        responseContext: ContainerResponseContext) {

        if (requestContext.uriInfo.path.startsWith(ES2PLUS_PATH_PREFIX)) {
            responseContext.headers.add("X-Admin-Protocol", X_ADMIN_PROTOCOL_HEADER_VALUE)
        }
    }
}

/**
 * Invoked when an exception is thrown when handling an ES2+ request.
 * The return value will be a perfectly normal "200" message, since that
 * is what the SM-DP+ standard requires.   This means we must ourselves
 * take the responsibility to log the situation as an error, otherwise it
 * will be very difficult to find it in the server logs.
 */
class SmdpExceptionMapper : ExceptionMapper<SmDpPlusException> {

    private val logger = LoggerFactory.getLogger(SmdpExceptionMapper::class.java)

    override fun toResponse(ex: SmDpPlusException): Response {

        // First we log the event.
        logger.error("SM-DP+ processing failed: {}" , ex.statusCodeData)

        // Then we prepare a response that will be returned to
        // whoever invoked the resource.
        val entity = HeaderOnlyResponse(
                header = newErrorHeader(ex))

        return Response.status(Response.Status.OK)
                .entity(entity)
                .type(MediaType.APPLICATION_JSON).build()
    }
}

///
///  The web resource using the protocol domain model.
///

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path(ES2PLUS_PATH_PREFIX)
class SmDpPlusServerResource(private val smDpPlus: SmDpPlusService) {

    companion object {
        const val ES2PLUS_PATH_PREFIX : String = "gsma/rsp2/es2plus/"
    }

    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
    @Path("downloadOrder")
    @POST
    fun downloadOrder(order: Es2PlusDownloadOrder): Es2DownloadOrderResponse {
        return smDpPlus.downloadOrder(
                eid = order.eid,
                iccid = order.iccid,
                profileType = order.profileType
        )
    }

    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
    @Path("confirmOrder")
    @POST
    fun confirmOrder(order: Es2ConfirmOrder): Es2ConfirmOrderResponse {
        return smDpPlus.confirmOrder(
                eid=order.eid,
                iccid = order.iccid,
                confirmationCode = order.confirmationCode,
                smdsAddress = order.smdpAddress,
                machingId = order.matchingId,
                releaseFlag =  order.releaseFlag
        )
    }

    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
    @Path("cancelOrder")
    @POST
    fun cancelOrder(order: Es2CancelOrder): HeaderOnlyResponse {

        smDpPlus.cancelOrder(
                eid = order.eid,
                iccid = order.iccid,
                matchingId = order.matchingId,
                finalProfileStatusIndicator = order.finalProfileStatusIndicator)
        return HeaderOnlyResponse()
    }

    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
    @Path("releaseProfile")
    @POST
    fun releaseProfile(order: Es2ReleaseProfile): HeaderOnlyResponse {

        smDpPlus.releaseProfile(iccid = order.iccid)
        return HeaderOnlyResponse()
    }

    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
    @Path("getProfileStatus")
    @POST
    fun getProfileStatus(order: Es2ProfileStatusCommand): Es2ProfileStatusResponse {
        return smDpPlus.getProfileStatus(iccidList = order.iccidList)
    }
}


@Path("/gsma/rsp2/es2plus/")
class SmDpPlusCallbackResource(private val smDpPlus: SmDpPlusCallbackService) {

    /**
     * This method is intended to be called _by_ the SM-DP+, sending information
     * back to the  operator's BSS system about the progress of various
     * operations.
     */
    @Critical
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("handleDownloadProgressInfo")
    @POST
    fun handleDownloadProgressInfo(order: Es2HandleDownloadProgressInfo): Response {
        smDpPlus.handleDownloadProgressInfo(
                header = order.header,
                eid = order.eid,
                iccid = order.iccid,
                profileType = order.profileType,
                timestamp = order.timestamp,
                notificationPointId = order.notificationPointId,
                notificationPointStatus = order.notificationPointStatus,
                resultData = order.resultData,
                imei = order.imei
        )

        /* According to the SM-DP+ spec. the response should 204. */
        return Response.status(Response.Status.NO_CONTENT)
                .build()
    }
}