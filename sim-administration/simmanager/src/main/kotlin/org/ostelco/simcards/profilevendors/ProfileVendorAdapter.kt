package org.ostelco.simcards.profilevendors

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.getLogger
import org.ostelco.prime.simmanager.AdapterError
import org.ostelco.prime.simmanager.NotFoundError
import org.ostelco.prime.simmanager.NotUpdatedError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.sim.es2plus.ES2RequestHeader
import org.ostelco.sim.es2plus.Es2ConfirmOrder
import org.ostelco.sim.es2plus.Es2ConfirmOrderResponse
import org.ostelco.sim.es2plus.Es2DownloadOrderResponse
import org.ostelco.sim.es2plus.Es2PlusDownloadOrder
import org.ostelco.sim.es2plus.Es2ProfileStatusCommand
import org.ostelco.sim.es2plus.Es2ProfileStatusResponse
import org.ostelco.sim.es2plus.FunctionExecutionStatus
import org.ostelco.sim.es2plus.FunctionExecutionStatusType
import org.ostelco.sim.es2plus.IccidListEntry
import org.ostelco.sim.es2plus.ProfileStatus
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SmDpPlusState
import javax.ws.rs.core.MediaType

/**
 * An profile vendors that can connect to SIM profile vendors and activate
 * the requested SIM profile.
 *
 * Will connect to the SM-DP+  and then activate the profile, so that when
 * user equpiment tries to download a profile, it will get a profile to
 * download.
 *
 * TODO:  Why on earth is the json property set to "metricName"? It makes no sense.  Fix it!
 */
data class ProfileVendorAdapter(
        @JsonProperty("id") val id: Long,
        @JsonProperty("metricName") val name: String) {

    private val logger by getLogger()

    /* For payload serializing. */
    private val mapper = jacksonObjectMapper()


    //  This class is currently the target of an ongoing refactoring.
    //   * First refactor confirmOrder and downloadOrder extensively,
    //     to ensure that any true differences stand out clearly, and repeated code
    //     is kept elsewhere.
    //   * Incredibly important, but only apparent after several rounds of initial refactoring:
    //         => Make the control flow clear!
    //   * Then  replace both with invocations to the possibly updated
    //     ES2+ client library.
    //   * Ensure that the protocol is extensively unit tested.

    private fun <T> buildEs2plusRequest(endpoint: String, esplusOrderName: String, payload: T): HttpUriRequest {
        val payloadString = mapper.writeValueAsString(payload)
        return RequestBuilder.post()
                .setUri("${endpoint}/gsma/rsp2/es2plus/${esplusOrderName}")
                .setHeader("User-Agent", "gsma-rsp-lpad")
                .setHeader("X-Admin-Protocol", "gsma/rsp/v2.0.0")
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setEntity(StringEntity(payloadString))
                .build()
    }

    /**
     * Requests the an external Profile Vendor to activate the
     * SIM profile.
     * @param httpClient  HTTP client
     * @param config SIM vendor specific configuration
     * @param dao  DB interface
     * @param eid  ESIM id
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    fun activate(httpClient: CloseableHttpClient,
                 config: ProfileVendorConfig,
                 dao: SimInventoryDAO,
                 eid: String? = null,
                 simEntry: SimEntry): Either<SimManagerError, SimEntry> =
            downloadOrder(httpClient, config, dao, simEntry)
                    .flatMap {
                        confirmOrder(httpClient, config, dao, eid, it)
                    }

    /**
     * Initiate activation of a SIM profile with an external Profile Vendor
     * by sending a SM-DP+ 'download-order' message.
     * @param httpClient  HTTP client
     * @param config SIM vendor specific configuration
     * @param dao  DB interface
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    private fun downloadOrder(httpClient: CloseableHttpClient,
                              config: ProfileVendorConfig,
                              dao: SimInventoryDAO,
                              simEntry: SimEntry): Either<SimManagerError, SimEntry> {

        val header = ES2RequestHeader(
                functionRequesterIdentifier = config.requesterIdentifier)
        val request =
                buildEs2plusRequest<Es2PlusDownloadOrder>(config.getEndpoint(), "downloadOrder",
                        Es2PlusDownloadOrder(
                                header = header,
                                iccid = simEntry.iccid
                        ))

        return try {
            httpClient.execute(request).use {
                when (it.statusLine.statusCode) {
                    200 -> {
                        val response = mapper.readValue(it.entity.content, Es2DownloadOrderResponse::class.java)

                        if (executionWasFailure(status = response.header.functionExecutionStatus)) {
                            logger.error("SM-DP+ 'order-download' message to service {} for ICCID {} failed with execution status {} (call-id: {}, uri = {})",
                                    config.name,
                                    simEntry.iccid,
                                    response.header.functionExecutionStatus,
                                    header.functionCallIdentifier,
                                    uri)
                            NotUpdatedError("SM-DP+ 'order-download' to ${config.name} failed with status: ${response.header.functionExecutionStatus}")
                                    .left()
                        } else {
                            if (simEntry.id == null) {
                                NotUpdatedError("simEntry without id.  simEntry=$simEntry").left()
                            } else {
                                dao.setSmDpPlusState(simEntry.id, SmDpPlusState.ALLOCATED)
                            }
                        }
                    }
                    else -> {
                        logger.error("SM-DP+ 'order-download' message to service {} for ICCID {} failed with status code {} (call-id: {})",
                                config.name,
                                simEntry.iccid,
                                it.statusLine.statusCode,
                                header.functionCallIdentifier)
                        NotUpdatedError("SM-DP+ 'order-download' to ${config.name} failed with code: ${it.statusLine.statusCode}")
                                .left()
                    }
                }
            }
        } catch (e: Throwable) {
            logger.error("SM-DP+ 'order-download' message to service {} for ICCID {} failed with error: {}",
                    config.name,
                    simEntry.iccid,
                    e)
            AdapterError("SM-DP+ 'order-download' message to service ${config.name} failed with error: $e")
                    .left()
        }
    }


    /**
     * Complete the activation of a SIM profile with an external Profile Vendor
     * by sending a SM-DP+ 'confirmation' message.
     * @param httpClient  HTTP client
     * @param config SIM vendor specific configuration
     * @param dao  DB interface
     * @param eid  ESIM id
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    private fun confirmOrder(httpClient: CloseableHttpClient,
                             config: ProfileVendorConfig,
                             dao: SimInventoryDAO,
                             eid: String? = null,
                             simEntry: SimEntry): Either<SimManagerError, SimEntry> {


        val header = ES2RequestHeader(functionRequesterIdentifier = config.requesterIdentifier)
        val iccid = simEntry.iccid
        val request =
                buildEs2plusRequest<Es2ConfirmOrder>(config.getEndpoint(), "confirmOrder",
                        Es2ConfirmOrder(
                                header = header,
                                eid = eid,
                                iccid = iccid
                        ))
        val functionCallIdentifier = header.functionCallIdentifier
        val nameOfSimVendor = config.name


        // TODO: This is is in the middle of a refactoring. We're trying to ensure that exits
        // are signalled clearly in the execution flow.   This is currently at the _cost_ of
        // making the code more verbose, but that is a cost we'll cut down on later, and certainly
        // before merging to develop.

        val response: CloseableHttpResponse
        try {
            response = httpClient.execute(request)
        } catch (e: Throwable) {
            logger.error("SM-DP+ 'order-confirm' message to service {} for ICCID {} failed with error: {}",
                    nameOfSimVendor,
                    iccid,
                    e)
            return AdapterError("SM-DP+ 'order-confirm' message to service $nameOfSimVendor failed with error: $e")
                    .left()
        }

        return try {

            if (response.statusLine.statusCode != 200) {
                logger.error("SM-DP+ 'order-confirm' message to service {} for ICCID {} failed with status code {} (call-id: {})",
                        nameOfSimVendor,
                        iccid,
                        response?.statusLine?.statusCode,
                        functionCallIdentifier)
                return NotUpdatedError("SM-DP+ 'order-confirm' to $nameOfSimVendor failed with code: ${response.statusLine.statusCode}")
                        .left()
            }


            val status = mapper.readValue(response.entity.content, Es2ConfirmOrderResponse::class.java)
            val functionExecutionStatus = status.header.functionExecutionStatus

            if (executionWasFailure(functionExecutionStatus)) {
                logger.error("SM-DP+ 'order-confirm' message to service {} for ICCID {} failed with execution status {} (call-id: {})",
                        nameOfSimVendor,
                        iccid,
                        functionExecutionStatus,
                        functionCallIdentifier)
                return NotUpdatedError("SM-DP+ 'order-confirm' to $nameOfSimVendor failed with status: $functionExecutionStatus")
                        .left()
            }

            if (status.eid.isNullOrEmpty()) {
                logger.warn("No EID returned from service {} for ICCID {} for SM-DP+ 'order-confirm' message (call-id: {})",
                        nameOfSimVendor,
                        iccid,
                        functionCallIdentifier)
            } else {

                val simEntryId = simEntry.id
                val statusEid = status.eid

                when {
                    simEntryId == null -> AdapterError("simEntryId == null").left()
                    statusEid == null -> AdapterError("statusEid == null").left()
                    else -> dao.setEidOfSimProfile(simEntryId, statusEid)
                }
            }

            if (!eid.isNullOrEmpty() && eid != status.eid) {
                logger.warn("EID returned from service {} does not match provided EID ({} <> {}) in SM-DP+ 'order-confirm' message (call-id: {})",
                        nameOfSimVendor,
                        eid,
                        status.eid,
                        functionCallIdentifier)
            }

            logger.info("SM-DP+ 'order-confirm' message to service {} for ICCID {} completed OK (call-id: {})",
                    nameOfSimVendor,
                    iccid,
                    functionCallIdentifier)

            val simEntryId = simEntry.id
            val statusMatchingId = status.matchingId

            when {
                simEntryId == null -> AdapterError("simEntryId == null").left()
                statusMatchingId == null -> AdapterError("statusMatchingId == null").left()
                else -> dao.setSmDpPlusStateAndMatchingId(simEntryId, SmDpPlusState.RELEASED, statusMatchingId)
            }
        }

        // TODO: This catch should be completely superflous, since the only things that can fail
        //        above are things happening in this block, which (with the exception of the ddao invocations) calls nothing

        catch (e: Throwable) {
            logger.error("SM-DP+ 'order-confirm' message to service {} for ICCID {} failed with error: {}",
                    nameOfSimVendor,
                    iccid,
                    e)
            AdapterError("SM-DP+ 'order-confirm' message to service $nameOfSimVendor failed with error: $e")
                    .left()
        }
    }

    private fun executionWasFailure(status: FunctionExecutionStatus) =
            status.status != FunctionExecutionStatusType.ExecutedSuccess

    /**
     * Downloads the SM-DP+ 'profile status' information for an ICCID from
     * a SM-DP+ service.
     * @param httpClient  HTTP client
     * @param config  SIM vendor specific configuration
     * @param iccid  ICCID
     * @return SM-DP+ 'profile status' for ICCID
     */
    fun getProfileStatus(httpClient: CloseableHttpClient,
                         config: ProfileVendorConfig,
                         iccid: String): Either<SimManagerError, ProfileStatus> =
            getProfileStatus(httpClient, config, listOf(iccid))
                    .flatMap {
                        it.first().right()
                    }

    /**
     * Downloads the SM-DP+ 'profile status' information for a list of ICCIDs
     * from a SM-DP+ service.
     * @param httpClient  HTTP client
     * @param config  SIM vendor specific configuration
     * @param iccidList  list with ICCID
     * @return  A list with SM-DP+ 'profile status' information
     */
    private fun getProfileStatus(httpClient: CloseableHttpClient,
                                 config: ProfileVendorConfig,
                                 iccidList: List<String>): Either<SimManagerError, List<ProfileStatus>> {
        if (iccidList.isNullOrEmpty()) {
            logger.error("One or more ICCID values required in SM-DP+ 'profile-status' message to service {}",
                    config.name)
            return NotFoundError("").left()
        }


        val header = ES2RequestHeader(
                functionRequesterIdentifier = config.requesterIdentifier)

        val request =
                buildEs2plusRequest<Es2ProfileStatusCommand>(config.getEndpoint(), "getProfileStatus",
                        Es2ProfileStatusCommand(
                                header = header,
                                iccidList = iccidList.map { IccidListEntry(iccid = it) }
                        ))


        /* Pretty print version of ICCID list. */
        val iccids = iccidList.joinToString(prefix = "[", postfix = "]")
        val functionCallIdentifier = header.functionCallIdentifier

        return try {
            httpClient.execute(request).use {

                when (it.statusLine.statusCode) {
                    200 -> {
                        val status = mapper.readValue(it.entity.content, Es2ProfileStatusResponse::class.java)

                        if (status.header.functionExecutionStatus.status != FunctionExecutionStatusType.ExecutedSuccess) {

                            logger.error("SM-DP+ 'profile-status' message to service {} for ICCID {} failed with execution status {} (call-id: {})",
                                    config.name,
                                    iccids,
                                    status.header.functionExecutionStatus,
                                    functionCallIdentifier)
                            NotUpdatedError("SM-DP+ 'profile-status' to ${config.name} failed with status: ${status.header.functionExecutionStatus}",
                                    pingOk = true)
                                    .left()

                        } else {
                            logger.info("SM-DP+ 'profile-status' message to service {} for ICCID {} completed OK (call-id: {})",
                                    config.name,
                                    iccids,
                                    functionCallIdentifier)
                            val profileStatusList = status.profileStatusList

                            if (!profileStatusList.isNullOrEmpty())
                                profileStatusList.right()
                            else
                                NotFoundError("No information found for ICCID $iccids in SM-DP+ 'profile-status' message to service ${config.name}",
                                        pingOk = true)
                                        .left()
                        }
                    }
                    else -> {
                        logger.error("SM-DP+ 'profile-status' message to service {} for ICCID {} failed with status code {} (call-id: {})",
                                config.name,
                                iccids,
                                it.statusLine.statusCode,
                                functionCallIdentifier)
                        NotUpdatedError("SM-DP+ 'order-confirm' to ${config.name} failed with code: ${it.statusLine.statusCode}")
                                .left()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("SM-DP+ 'profile-status' message to service ${config.name} via endpoint '${config.getEndpoint()}' for ICCID ${iccids} failed with error.",
                    e)
            AdapterError("SM-DP+ 'profile-status' message to service ${config.name} failed with error: $e")
                    .left()
        }
    }

    /**
     * A dummy ICCID. May or may notreturn a valid profile from any HSS or SM-DP+, but is
     * useful for checking of there is an SM-DP+ in the other end of the connection.
     */
    val invalidICCID = listOf("8901000000000000001")

    /**
     * Contact the ES2+  endpoint of the SM-DP+, and return true if the answer indicates
     * that it's up.
     */
    fun ping(httpClient: CloseableHttpClient,
             config: ProfileVendorConfig): Either<SimManagerError, List<ProfileStatus>> =
            getProfileStatus(
                    httpClient = httpClient,
                    config = config,
                    iccidList = invalidICCID)
}