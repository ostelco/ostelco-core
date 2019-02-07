package org.ostelco.sim.es2plus

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


/**
 * A httpClient implementation that is able to talk all of the GSMA specified parts of
 * the ES2+ protocol.
 */
class ES2PlusClient(
        private val requesterId: String,
        private val host: String = "127.0.0.1",
        private val port: Int = 8443,
        private val httpClient: HttpClient? = null,
        private val jerseyClient: Client? = null) {

    companion object {
        const val X_ADMIN_PROTOCOL_HEADER_VALUE = "gsma/rsp/v2.0.0"
    }


    @Throws(ES2PlusClientException::class)
    private fun <T, S> postEs2ProtocolCmd(
            path: String,
            es2ProtocolPayload: T,
            sclass: Class<S>,
            expectedReturnCode: Int = 200): S {


        /// XXX TODO:
        //       We  currently need jersey client for integration test and  httpClient for functional
        //       SSL.  This is unfortunate, but also seems to be the shortest path towards a
        //       functioning & testable  ES2+ client.   Should be implemented using two different
        //       methods, that should then share a lot of common code.

        if (httpClient != null) {

            val objectMapper = ObjectMapper()
            val payload = objectMapper.writeValueAsString(es2ProtocolPayload)

            val req = HttpPost("https://%s:%d%s".format(host, port, path))

            req.setHeader("User-Agent", "gsma-rsp-lpad")
            req.setHeader("X-Admin-Protocol", X_ADMIN_PROTOCOL_HEADER_VALUE)
            req.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            req.setHeader("Accept", "application/json")
            req.setHeader("Content-type", "application/json")
            req.setEntity(StringEntity(payload))


            val result: HttpResponse = httpClient.execute(req) ?: throw ES2PlusClientException("Null response from http httpClient")

            // Validate returned response
            val statusCode = result.statusLine.statusCode
            if (expectedReturnCode != statusCode) {

                val msg = "Expected return value $expectedReturnCode, but got ${statusCode}.  Body was \"${result.entity.getContent()}\""
                throw ES2PlusClientException(msg)
            }

            val xAdminProtocolHeader = result.getFirstHeader("X-Admin-Protocol")!!

            if (xAdminProtocolHeader == null) {
                throw ES2PlusClientException("Expected header X-Admin-Protocol to be non null")
            }

            val protocolVersion = xAdminProtocolHeader.value

            if (protocolVersion != X_ADMIN_PROTOCOL_HEADER_VALUE) {
                throw ES2PlusClientException("Expected header X-Admin-Protocol to be '$X_ADMIN_PROTOCOL_HEADER_VALUE' but it was '$xAdminProtocolHeader'")
            }

            val returnedContentType = result.getFirstHeader("Content-Type")!!
            val expectedContentType = "application/json"

            if (returnedContentType == null || returnedContentType.value != expectedContentType) {
                throw ES2PlusClientException("Expected header Content-Type to be '$expectedContentType' but was '$returnedContentType'")
            }

            val returnValue = objectMapper.readValue(result.getEntity().getContent(), sclass) ?: throw ES2PlusClientException("null return value")
            return returnValue
        } else if (jerseyClient != null) {
            val entity: Entity<T> = Entity.entity(es2ProtocolPayload, MediaType.APPLICATION_JSON)
            val result: Response = jerseyClient.target(path)
                    .request(MediaType.APPLICATION_JSON)
                    .header("User-Agent", "gsma-rsp-lpad")
                    .header("X-Admin-Protocol", X_ADMIN_PROTOCOL_HEADER_VALUE)
                    .post(entity)

            // Validate returned response
            if (expectedReturnCode != result.status) {
                val msg = "Expected return value $expectedReturnCode, but got ${result.status}.  Body was \"${result.readEntity(String::class.java)}\""
                throw ES2PlusClientException(msg)
            }

            val xAdminProtocolHeader = result.getHeaderString("X-Admin-Protocol")

            if (xAdminProtocolHeader == null || xAdminProtocolHeader != X_ADMIN_PROTOCOL_HEADER_VALUE) {
                throw ES2PlusClientException("Expected header X-Admin-Protocol to be '$X_ADMIN_PROTOCOL_HEADER_VALUE' but it was '$xAdminProtocolHeader'")
            }

            val returnedContentType = result.getHeaderString("Content-Type")
            val expectedContentType = "application/json"

            if (returnedContentType == null || returnedContentType != expectedContentType) {
                throw ES2PlusClientException("Expected header Content-Type to be '$expectedContentType' but was '$returnedContentType'")
            }
            return result.readEntity(sclass)
        } else {
            throw RuntimeException("No jersey nor apache http client, bailing out!!")
        }
    }

    fun downloadOrder(
            eid: String? = null,
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

    fun confirmOrder(eid: String? = null,
                     iccid: String,
                     matchingId: String? = null,
                     confirmationCode: String? = null,
                     smdsAddress: String? = null,
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


/**
 * Configuration class to be used in application's config
 * when a client is necessary.
 */
class EsTwoPlusConfig {
    @Valid
    @NotNull
    @JsonProperty("requesterId")
    var requesterId: String = ""

    @Valid
    @NotNull
    @JsonProperty("host")
    var host: String = ""

    @Valid
    @NotNull
    @JsonProperty("port")
    var port: Int = 4711
}


