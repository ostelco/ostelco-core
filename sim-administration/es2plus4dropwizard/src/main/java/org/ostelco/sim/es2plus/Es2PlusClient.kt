package org.ostelco.sim.es2plus

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.methods.*
import org.apache.http.entity.StringEntity
import java.net.http.HttpRequest
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType




/**
 * A client implementation that is able to talk all of the GSMA specified parts of
 * the ES2+ protocol.
 */
class ES2PlusClient(
        private val requesterId: String,
        private val host: String = "127.0.0.1",
        private val port: Int = 8443,
        private val client: org.apache.http.client.HttpClient) {
    companion object {
        val X_ADMIN_PROTOCOL_HEADER_VALUE = "gsma/rsp/v<x.y.z>"
    }


    @Throws(ES2PlusClientException::class)
    private fun <T, S> postEs2ProtocolCmd(
            path: String,
            es2ProtocolPayload: T,
            sclass: Class<S>,
            expectedReturnCode: Int = 200): S {
        val entity: Entity<T> = Entity.entity(es2ProtocolPayload, MediaType.APPLICATION_JSON)

        val objectMapper  = ObjectMapper()
        val payload = objectMapper.writeValueAsString(es2ProtocolPayload)

       /* val request = HttpRequest.newBuilder()
                .header("User-Agent", "gsma-rsp-lpad")
                .header("X-Admin-Protocol", X_ADMIN_PROTOCOL_HEADER_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build() */


        val req = HttpPost("https://%s:%d%s".format(host, port, path))

        req.setHeader("User-Agent", "gsma-rsp-lpad")
        req.setHeader("X-Admin-Protocol", X_ADMIN_PROTOCOL_HEADER_VALUE)
        req.setHeader("Content-Type", MediaType.APPLICATION_JSON)
        req.setHeader("Accept", "application/json")
        req.setHeader("Content-type", "application/json")
        req.setEntity(StringEntity(payload))

        val result: HttpResponse =  client.execute(req)

        if (result == null) {
            throw ES2PlusClientException("Null response from http client")
        }

        /*
        val result: Response = client.target(path)
                .request(MediaType.APPLICATION_JSON)
                .header("User-Agent", "gsma-rsp-lpad")
                .header("X-Admin-Protocol", X_ADMIN_PROTOCOL_HEADER_VALUE)
                .post(entity)
                */

        // Validate returned response
        val statusCode = result.statusLine.statusCode
        if (expectedReturnCode != statusCode) {
            val msg = "Expected return value $expectedReturnCode, but got ${statusCode}.  Body was \"${result.getEntity().getContent()}\""
            throw ES2PlusClientException(msg)
        }

        val xAdminProtocolHeader = result.getFirstHeader("X-Admin-Protocol")!!
        if (xAdminProtocolHeader == null || !xAdminProtocolHeader.equals(X_ADMIN_PROTOCOL_HEADER_VALUE)) {
            throw ES2PlusClientException("Expected header X-Admin-Protocol to be '$X_ADMIN_PROTOCOL_HEADER_VALUE' but it was '$xAdminProtocolHeader'")
        }

        val returnedContentType = result.getFirstHeader("Content-Type")!!
        val expectedContentType = "application/json"
        if (returnedContentType == null || !returnedContentType.equals(expectedContentType)) {
            throw ES2PlusClientException("Expected header Content-Type to be '$expectedContentType' but was '$returnedContentType'")
        }

        val returnValue = objectMapper.readValue(result.getEntity().getContent(), sclass)

        if (returnValue == null) {
            throw ES2PlusClientException("null return value")
        }
        return returnValue
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

