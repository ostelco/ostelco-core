package org.ostelco.sim.es2plus

import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


/**
 * A client implementation that is able to talk all of the GSMA specified parts of
 * the ES2+ protocol.
 */
class ES2PlusClient(private val requesterId: String, private val client: Client) {
    companion object {
        val X_ADMIN_PROTOCOL_HEADER_VALUE = "gsma/rsp/v<x.y.z>"
    }


    @Throws(ES2PlusClientException::class)
    private fun <T, S> postEs2ProtocolCmd(
            path: String,
            es2ProtocolPayload: T,
            sclass: Class<S>,
            expectedReturnCode: Int = 201): S {
        val entity: Entity<T> = Entity.entity(es2ProtocolPayload, MediaType.APPLICATION_JSON)
        val result: Response = client.target(path)
                .request(MediaType.APPLICATION_JSON)
                .header("User-Agent", "gsma-rsp-lpad")
                .header("X-Admin-Protocol", X_ADMIN_PROTOCOL_HEADER_VALUE)
                .post(entity)

        // Validata returned response
        if (expectedReturnCode != result.status) {
            val msg = "Expected return value $expectedReturnCode, but got ${result.status}.  Body was \"${result.readEntity(String::class.java)}\""
            throw ES2PlusClientException(msg)
        }

        val xAdminProtocolHeader = result.getHeaderString("X-Admin-Protocol")
        if (xAdminProtocolHeader == null || !xAdminProtocolHeader.equals(X_ADMIN_PROTOCOL_HEADER_VALUE)) {
            throw ES2PlusClientException("Expected header X-Admin-Protocol to be '$X_ADMIN_PROTOCOL_HEADER_VALUE' but it was '$xAdminProtocolHeader'")
        }

        val returnedContentType = result.getHeaderString("Content-Type")

        val expectedContentType = "application/json"
        if (returnedContentType == null || !returnedContentType.equals(expectedContentType)) {
            throw ES2PlusClientException("Expected header Content-Type to be '$expectedContentType' but was '$returnedContentType'")
        }

        return result.readEntity(sclass)
    }

    fun downloadOrder(
            eid: String,
            iccid: String,
            profileType: String): Es2DownloadOrderResponse {
        val es2ProtocolPayload = Es2PlusDownloadOrder(
                header = ES2RequestHeader(
                        functionRequesterIdentifier = requesterId,
                        functionCallIdentifier = "downloadOrder"),
                eid = eid,
                iccid = iccid,
                profileType = profileType)

        return postEs2ProtocolCmd(
                "/gsma/rsp2/es2plus/downloadOrder",
                es2ProtocolPayload,
                Es2DownloadOrderResponse::class.java,
                expectedReturnCode = 200)
    }

    fun confirmOrder(eid: String,
                     iccid: String,
                     matchingId: String,
                     confirmationCode: String,
                     smdsAddress: String,
                     releaseFlag: Boolean): Es2ConfirmOrderResponse {
        val es2ProtocolPayload =
                Es2ConfirmOrder(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = requesterId,
                                functionCallIdentifier = "confirmOrder"),
                        eid = eid,
                        iccid = iccid,
                        matchingId = matchingId,
                        confirmationCode = confirmationCode,
                        smdsAddress = smdsAddress,
                        releaseFlag = releaseFlag)
        return postEs2ProtocolCmd(
                "/gsma/rsp2/es2plus/confirmOrder",
                es2ProtocolPayload = es2ProtocolPayload,
                expectedReturnCode = 200,
                sclass = Es2ConfirmOrderResponse::class.java)
    }

    fun cancelOrder(eid: String,
                    iccid: String,
                    matchingId: String,
                    finalProfileStatusIndicator: String): HeaderOnlyResponse {
        return postEs2ProtocolCmd("/gsma/rsp2/es2plus/cancelOrder",
                es2ProtocolPayload = Es2CancelOrder(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = requesterId,
                                functionCallIdentifier = "cancelOrder"
                        ),
                        eid = eid,
                        iccid = iccid,
                        matchingId = matchingId,
                        finalProfileStatusIndicator = finalProfileStatusIndicator),
                sclass = HeaderOnlyResponse::class.java,
                expectedReturnCode = 200)
    }

    fun releaseProfile(iccid: String): HeaderOnlyResponse {
        return postEs2ProtocolCmd("/gsma/rsp2/es2plus/releaseProfile",
                Es2ReleaseProfile(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = requesterId,
                                functionCallIdentifier = "releaseProfile"
                        ),
                        iccid = iccid),
                sclass = HeaderOnlyResponse::class.java,
                expectedReturnCode = 200)
    }


    fun handleDownloadProgressInfo(
            eid: String? = null,
            iccid: String,
            profileType: String,
            timestamp: String,
            notificationPointId: Int,
            notificationPointStatus: ES2NotificationPointStatus,
            resultData: String? = null,
            imei: String? = null
    ): HeaderOnlyResponse {
        return postEs2ProtocolCmd("/gsma/rsp2/es2plus/handleDownloadProgressInfo",
                Es2HandleDownloadProgressInfo(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = requesterId,
                                functionCallIdentifier = "handleDownloadProgressInfo"

                        ),
                        eid = eid,
                        iccid = iccid,
                        profileType = profileType,
                        timestamp = timestamp,
                        notificationPointId = notificationPointId,
                        notificationPointStatus = notificationPointStatus,
                        resultData = resultData,
                        imei = imei),
                sclass = HeaderOnlyResponse::class.java,
                expectedReturnCode = 200)
    }
}


/**
 * Thrown when something goes wrong with the ES2+ protocol.
 */
class ES2PlusClientException(msg: String) : Exception(msg)

