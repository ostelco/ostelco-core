package org.ostelco.sim.es2plus

import io.dropwizard.jersey.setup.JerseyEnvironment
import org.ostelco.jsonschema.RequestServerReaderWriterInterceptor
import org.ostelco.sim.es2plus.ES2PlusClient.Companion.X_ADMIN_PROTOCOL_HEADER_VALUE
import org.ostelco.sim.es2plus.SmDpPlusServerResource.Companion.ES2PLUS_PATH_PREFIX
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

    companion object {
        fun addEs2PlusDefaultFiltersAndInterceptors(env: JerseyEnvironment) {
            env.register(ES2PlusIncomingHeadersFilter())
            env.register(ES2PlusOutgoingHeadersFilter())
            env.register(RequestServerReaderWriterInterceptor())
            env.register(SmdpExceptionMapper())
        }
    }

    @Throws(IOException::class)
    override fun filter(ctx: ContainerRequestContext) {


        if (!ctx.uriInfo.path.startsWith(ES2PLUS_PATH_PREFIX)) {
            return
        }

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

@Provider
class ES2PlusOutgoingHeadersFilter : ContainerResponseFilter {

    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext,
                        responseContext: ContainerResponseContext) {
        responseContext.headers.add("X-Admin-Protocol", X_ADMIN_PROTOCOL_HEADER_VALUE)
    }
}

class SmdpExceptionMapper : ExceptionMapper<SmDpPlusException> {
    override fun toResponse(ex: SmDpPlusException): Response {

        // XXX Use some other responser than this, just a placeholderr
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
        const val ES2PLUS_PATH_PREFIX : String = "/gsma/rsp2/es2plus/"
    }

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

        return Es2DownloadOrderResponse(iccid = iccid)
    }

    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
    @Path("confirmOrder")
    @POST
    fun confirmOrder(order: Es2ConfirmOrder): Es2ConfirmOrderResponse {
        return Es2ConfirmOrderResponse(
                eid = order.eid,
                smdsAddress = order.smdsAddress,
                matchingId = order.matchingId)
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
}

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/gsma/rsp2/es2plus/")
class SmDpPlusCallbackResource(private val smDpPlus: SmDpPlusCallbackService) {

    /**
     * This method is intended to be called _by_ the SM-DP+, sending information
     * back to the  operator's BSS system about the progress of various
     * operations.
     */
    @Path("handleDownloadProgressInfo")
    @POST
    fun handleDownloadProgressInfo(order: Es2HandleDownloadProgressInfo): HeaderOnlyResponse {
        smDpPlus.handleDownloadProgressInfo(
                eid = order.eid,
                iccid = order.iccid,
                notificationPointId = order.notificationPointId,
                profileType = order.profileType,
                resultData = order.resultData,
                timestamp = order.timestamp
        )
        return HeaderOnlyResponse()
    }
}