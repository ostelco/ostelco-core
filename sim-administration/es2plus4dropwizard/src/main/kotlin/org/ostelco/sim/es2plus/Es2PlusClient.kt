package org.ostelco.sim.es2plus

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.ostelco.prime.getLogger
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
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
        private val useHttps: Boolean = true,
        private val jerseyClient: Client? = null) {

    // TODO: Make a new interface to represent the client we are using, then set a non-nullable field
    //       (private) to contain one instande that represents either a HttpClient or a jerseyClient
    //       wrapped in the appropriate protocol logic.   This will get rid of the silly ==null tests
    //       that riddle this class.

    /**
     * The logger, to which we log.
     */
    val log = getLogger()

    companion object {

        /**
         * Protocol header value used to identify http request as ES2+
         */
        const val X_ADMIN_PROTOCOL_HEADER_VALUE = "gsma/rsp/v2.0.0"

        /**
         *  The name the ES2+ client will announce it self as.
         */
        const val CLIENT_USER_AGENT = "gsma-rsp-lpad"

        /**
         * Format zoned time as..
         *    ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[T,D,Z]{1}$
         */
        fun getDatetime(time: ZonedDateTime) =
                DateTimeFormatter.ofPattern("YYYY-MM-dd'T'hh:mm:ss'Z'").format(time)

        /**
         *  Get current time as a string that can be used as  a timestamp in
         *   ES2+ protocol entities.
         */
        fun getNowAsDatetime(): String = getDatetime(ZonedDateTime.now())

        /**
         *  Function call identifiers are used to uniquely ientify ES2+ method invocations.
         */
        fun newRandomFunctionCallIdentifier() = UUID.randomUUID().toString()
    }

    private fun constructUrl(path: String): String {
        val prefix = if (useHttps) "https" else "http"
        return "%s://%s:%d%s".format(prefix, host, port, path)
    }

    private fun <T, S> getEs2PlusHttpPostReturnValue(
            path: String,
            es2ProtocolPayload: T,
            returnValueClass: Class<S>,
            expectedStatusCode: Int = 200): S {

        // Execute the POST
        val response: HttpResponse = getEs2PlusHttpPostResponse(path, es2ProtocolPayload, expectedStatusCode)

        // Validate the content type
        val returnedContentType = response.getFirstHeader("Content-Type")
        val expectedContentType = MediaType.APPLICATION_JSON

        if (returnedContentType.value != expectedContentType) {
            throw ES2PlusClientException("Expected header Content-Type to be '$expectedContentType' but was '$returnedContentType'")
        }

        // Looks legit, deserialize into return value class using Jackson object mapper
        return ObjectMapper().readValue(response.entity.content, returnValueClass)
                ?: throw ES2PlusClientException("null return value")
    }

    private fun <T> getEs2PlusHttpPostResponse(path: String, es2ProtocolPayload: T, expectedStatusCode: Int): HttpResponse {
        val req = constructEs2PlusPostRequest(path, es2ProtocolPayload)
        val response = executeRequestWithRetries(req)
        validateEs2plusResponse(response, expectedStatusCode)
        return response
    }

    // Sometimes the ES2+ SSL connection is slow, so we want to retry
    // This method will try three times with one second between attempts
    // (only after a timeout failure, which in itself could have taken some time).
    private fun executeRequestWithRetries(req: HttpPost): HttpResponse {

        if (httpClient == null) {
            throw ES2PlusClientException("Attempt to use http client, even though it is not present in the ES2+ client.")
        }

        val maxRetries = 3
        val retryIntervalInMilliseconds = 1000L

        var returnedResponse: HttpResponse? = null
        for (i in 0..maxRetries) {
            try {
                returnedResponse = httpClient.execute(req)
                        ?: throw ES2PlusClientException("Null response from http httpClient")
                break
            } catch (e: java.net.SocketTimeoutException) {
                log.value.info("Timeout while attempting ES2+ request $req.   Retrying")
                try {
                    Thread.sleep(retryIntervalInMilliseconds)
                } catch (e2: InterruptedException) {
                    // Move along
                }
            }
        }

        if (returnedResponse == null) {
            throw ES2PlusClientException("Timeout after trying $maxRetries attempts with $retryIntervalInMilliseconds milliseconds between them")
        }
        return returnedResponse
    }

    private fun validateEs2plusResponse(response: HttpResponse, expectedStatusCode: Int) {
        // Validate returned response
        val statusCode = response.statusLine.statusCode
        if (expectedStatusCode != statusCode) {
            val msg = "Expected return value $expectedStatusCode, but got $statusCode.  Body was \"${response.entity.content}\""
            throw ES2PlusClientException(msg)
        }

        val xAdminProtocolHeader = response.getFirstHeader("X-Admin-Protocol")
                ?: throw ES2PlusClientException("Expected header X-Admin-Protocol to be non null")

        val protocolVersion = xAdminProtocolHeader.value

        if (protocolVersion != X_ADMIN_PROTOCOL_HEADER_VALUE) {
            throw ES2PlusClientException("Expected header X-Admin-Protocol to be '$X_ADMIN_PROTOCOL_HEADER_VALUE' but it was '$xAdminProtocolHeader'")
        }
    }

    private fun <T> constructEs2PlusPostRequest(path: String, es2ProtocolPayload: T): HttpPost {
        val url = constructUrl(path)
        val req = HttpPost(url)

        val objectMapper = ObjectMapper()
        val payload = objectMapper.writeValueAsString(es2ProtocolPayload)

        req.setHeader("User-Agent", CLIENT_USER_AGENT)
        req.setHeader("X-Admin-Protocol", X_ADMIN_PROTOCOL_HEADER_VALUE)
        req.setHeader("Content-Type", MediaType.APPLICATION_JSON)
        req.setHeader("Accept", MediaType.APPLICATION_JSON)
        req.entity = StringEntity(payload)
        return req
    }

    /* For test cases where content should be returned. */
    @Throws(ES2PlusClientException::class)
    private fun <T, S> postEs2ProtocolCmd(
            path: String,
            es2ProtocolPayload: T,
            returnValueClass: Class<S>,
            expectedStatusCode: Int = 200): S {

        /// XXX TODO:
        //       We  currently need jersey client for integration test and  httpClient for functional
        //       SSL.  This is unfortunate, but also seems to be the shortest path towards a
        //       functioning & testable  ES2+ client.   Should be implemented using two different
        //       methods, that should then share a lot of common code.

        if (httpClient != null) {
            return getEs2PlusHttpPostReturnValue(path, es2ProtocolPayload, returnValueClass = returnValueClass, expectedStatusCode = expectedStatusCode)
        } else if (jerseyClient != null) {
            val entity: Entity<T> = Entity.entity(es2ProtocolPayload, MediaType.APPLICATION_JSON)
            val result: Response = jerseyClient.target(path)
                    .request(MediaType.APPLICATION_JSON)
                    .header("User-Agent", CLIENT_USER_AGENT)
                    .header("X-Admin-Protocol", X_ADMIN_PROTOCOL_HEADER_VALUE)
                    .post(entity)

            // Validate returned response
            if (expectedStatusCode != result.status) {
                val msg = "Expected return value $expectedStatusCode, but got ${result.status}.  Body was \"${result.readEntity(String::class.java)}\""
                throw ES2PlusClientException(msg)
            }

            val xAdminProtocolHeader = result.getHeaderString("X-Admin-Protocol")

            if (xAdminProtocolHeader == null || xAdminProtocolHeader != X_ADMIN_PROTOCOL_HEADER_VALUE) {
                throw ES2PlusClientException("Expected header X-Admin-Protocol to be '$X_ADMIN_PROTOCOL_HEADER_VALUE' but it was '$xAdminProtocolHeader'")
            }

            val returnedContentType = result.getHeaderString("Content-Type")
            val expectedContentType = MediaType.APPLICATION_JSON

            if (returnedContentType == null || returnedContentType != expectedContentType) {
                throw ES2PlusClientException("Expected header Content-Type to be '$expectedContentType' but was '$returnedContentType'")
            }
            return result.readEntity(returnValueClass)
        } else {
            throw RuntimeException("No jersey nor apache http client, bailing out!!")
        }
    }

    /* For cases where no content should be returned. Currently only
       used in 'progress-download' test. */
    @Throws(ES2PlusClientException::class)
    private fun <T> postEs2ProtocolCmdNoContentReturned(
            path: String,
            es2ProtocolPayload: T,
            expectedReturnCode: Int = 204) {
        if (httpClient != null) {
            getEs2PlusHttpPostResponse(path, es2ProtocolPayload, expectedReturnCode)
        } else if (jerseyClient != null) {
            val entity: Entity<T> = Entity.entity(es2ProtocolPayload, MediaType.APPLICATION_JSON)
            val result: Response = jerseyClient.target(path)
                    .request(MediaType.APPLICATION_JSON)
                    .header("User-Agent", CLIENT_USER_AGENT)
                    .header("X-Admin-Protocol", X_ADMIN_PROTOCOL_HEADER_VALUE)
                    .post(entity)

            // Validate returned response
            if (expectedReturnCode != result.status) {
                val msg = "Expected return value $expectedReturnCode, but got ${result.status}."
                throw ES2PlusClientException(msg)
            }
        } else {
            throw RuntimeException("No jersey nor apache http client, bailing out!!")
        }
    }

    /**
     * Given a list of ICCID values, will package it as an ES2+ ProfileStatus message,
     * send it along to the service in the other end, and parse the result.
     */
    fun profileStatus(
            iccidList: List<String>): Es2ProfileStatusResponse {

        val wrappedIccidList = iccidList.map { IccidListEntry(iccid = it) }

        val es2ProtocolPayload = Es2PlusProfileStatus(
                header = ES2RequestHeader(
                        functionRequesterIdentifier = requesterId),
                iccidList = wrappedIccidList)

        return postEs2ProtocolCmd(
                "/gsma/rsp2/es2plus/getProfileStatus",
                es2ProtocolPayload,
                Es2ProfileStatusResponse::class.java,
                expectedStatusCode = 200)
    }

    /**
     * Given  ICCID value (and an optional and seldom used eid value),
     * will package it as an ES2+ downloadOrder message,
     * send it along to the service in the other end, and parse the result.
     */
    fun downloadOrder(
            eid: String? = null,
            iccid: String,
            profileType: String? = null): Es2DownloadOrderResponse {
        val es2ProtocolPayload = Es2PlusDownloadOrder(
                header = ES2RequestHeader(
                        functionRequesterIdentifier = requesterId,
                        functionCallIdentifier = newRandomFunctionCallIdentifier()),
                eid = eid,
                iccid = iccid,
                profileType = profileType)

        return postEs2ProtocolCmd(
                "/gsma/rsp2/es2plus/downloadOrder",
                es2ProtocolPayload,
                Es2DownloadOrderResponse::class.java,
                expectedStatusCode = 200)
    }


    /**
     * Exceute an ES2+ confirmOrder.
     */
    fun confirmOrder(eid: String? = null,
                     iccid: String,
                     matchingId: String? = null,
                     confirmationCode: String? = null,
                     smdpAddress: String? = null,
                     releaseFlag: Boolean): Es2ConfirmOrderResponse {
        val es2ProtocolPayload =
                Es2ConfirmOrder(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = requesterId),
                        eid = eid,
                        iccid = iccid,
                        matchingId = matchingId,
                        confirmationCode = confirmationCode,
                        smdpAddress = smdpAddress,
                        releaseFlag = releaseFlag)
        return postEs2ProtocolCmd(
                "/gsma/rsp2/es2plus/confirmOrder",
                es2ProtocolPayload = es2ProtocolPayload,
                expectedStatusCode = 200,
                returnValueClass = Es2ConfirmOrderResponse::class.java)
    }

    /**
     * Excecute an ES2+ cancelOrder.
     */
    fun cancelOrder(iccid: String, finalProfileStatusIndicator: String, eid: String? = null, matchingId: String? = null): HeaderOnlyResponse {
        return postEs2ProtocolCmd("/gsma/rsp2/es2plus/cancelOrder",
                es2ProtocolPayload = Es2CancelOrder(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = requesterId),
                        iccid = iccid,
                        eid = eid,
                        matchingId = matchingId,
                        finalProfileStatusIndicator = finalProfileStatusIndicator),
                returnValueClass = HeaderOnlyResponse::class.java,
                expectedStatusCode = 200)
    }

    /**
     * Excecute an ES2+ releaseProfile.
     */
    fun releaseProfile(iccid: String): HeaderOnlyResponse {
        return postEs2ProtocolCmd("/gsma/rsp2/es2plus/releaseProfile",
                Es2ReleaseProfile(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = requesterId),
                        iccid = iccid),
                returnValueClass = HeaderOnlyResponse::class.java,
                expectedStatusCode = 200)
    }


    /**
     * Excecute an ES2+ handleDownloadProgressInfo towards the service in the other end.
     */
    fun handleDownloadProgressInfo(
            eid: String? = null,
            iccid: String,
            profileType: String,
            timestamp: String = getNowAsDatetime(),
            notificationPointId: Int,
            notificationPointStatus: ES2NotificationPointStatus,
            resultData: String? = null,
            imei: String? = null
    ) {
        postEs2ProtocolCmdNoContentReturned("/gsma/rsp2/es2plus/handleDownloadProgressInfo",
                Es2HandleDownloadProgressInfo(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = requesterId),
                        eid = eid,
                        iccid = iccid,
                        profileType = profileType,
                        timestamp = timestamp,
                        notificationPointId = notificationPointId,
                        notificationPointStatus = notificationPointStatus,
                        resultData = resultData,
                        imei = imei),
                expectedReturnCode = 204)
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

    /**
     * A string sent over the wire to identify the requester.
     */
    @Valid
    @NotNull
    @JsonProperty("requesterId")
    var requesterId: String = ""

    /**
     * IP host of the ES2+ server
     */
    @Valid
    @NotNull
    @JsonProperty("host")
    var host: String = ""

    /**
     * Port of the ES2+ server.  Default is 4711 which is almost
     * certainly wrong for any real server.
     */
    @Valid
    @NotNull
    @JsonProperty("port")
    var port: Int = 4711
}
