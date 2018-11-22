package org.ostelco.simcards.es2plus

import java.io.IOException
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider


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

class SmdpExceptionMapper : ExceptionMapper<SmDpPlusException> {
    override fun toResponse(ex: SmDpPlusException): Response {

        // XXX Use some other responser than this, just a placeholderr
        val entity = Es2ReleaseProfileResponse(
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
@Path("/gsma/rsp2/es2plus/")
class SmDpPlusServerResource(private val smDpPlus: SmDpPlusService) {

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
    fun cancelOrder(order: Es2CancelOrder): Es2CancelOrderResponse {

        smDpPlus.cancelOrder(
                eid = order.eid,
                iccid = order.iccid,
                matchingId = order.matchingId,
                finalProfileStatusIndicator = order.finalProfileStatusIndicator)
        return Es2CancelOrderResponse()
    }

    /**
     * Provided by SM-DP+, called by operator's BSS system.
     */
    @Path("releaseProfile")
    @POST
    fun releaseProfile(order: Es2ReleaseProfile): Es2ReleaseProfileResponse {

        smDpPlus.releaseProfile(iccid = order.iccid)
        return Es2ReleaseProfileResponse()
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
    fun handleDownloadProgressInfo(order: Es2HandleDownloadProgressInfo): Es2HandleDownloadProgressInfoResponse {
        smDpPlus.handleDownloadProgressInfo(
                eid = order.eid,
                iccid = order.iccid,
                notificationPointId = order.notificationPointId,
                profileType = order.profileType,
                resultData = order.resultData,
                timestamp = order.timestamp
        )
        return Es2HandleDownloadProgressInfoResponse()
    }
}