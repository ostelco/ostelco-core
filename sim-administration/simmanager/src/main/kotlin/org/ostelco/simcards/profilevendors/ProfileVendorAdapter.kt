package org.ostelco.simcards.profilevendors

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import org.ostelco.sim.es2plus.Es2PlusProfileStatus
import org.ostelco.sim.es2plus.Es2ProfileStatusResponse
import org.ostelco.sim.es2plus.FunctionExecutionStatusType
import org.ostelco.sim.es2plus.IccidListEntry
import org.ostelco.sim.es2plus.ProfileStatus
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SmDpPlusState
import java.util.*
import javax.ws.rs.core.MediaType

/**
 * An profile vendors that can connect to SIM profile vendors and activate
 * the requested SIM profile.
 *
 * Will connect to the SM-DP+  and then activate the profile, so that when
 * user equpiment tries to download a profile, it will get a profile to
 * download.
 *
 * XXX Why on earth is the json property set to "metricName"? It makes no sense.
 */
data class ProfileVendorAdapter(
        @JsonProperty("id") val id: Long,
        @JsonProperty("metricName") val name: String) {

    private val logger by getLogger()

    /* For payload serializing. */
    private val mapper = jacksonObjectMapper()

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
        val body = Es2PlusDownloadOrder(
                header = header,
                iccid = simEntry.iccid
        )
        val payload = mapper.writeValueAsString(body)

        // XXX Make it so that the config for acceptance, unit and integration tests are the same, otherwise
        //     the thing wil break spectacularly. Consider adding a test to ensure that no path elements
        //     are made part of the es2plusEndpoint.

        val uri = "${config.es2plusEndpoint}/gsma/rsp2/es2plus/downloadOrder"
        logger.info("URI  for downloadOrder = '$uri'")

        val request = RequestBuilder.post()
                .setUri(uri)
                .setHeader("User-Agent", "gsma-rsp-lpad")
                .setHeader("X-Admin-Protocol", "gsma/rsp/v2.0.0")
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setEntity(StringEntity(payload))
                .build()

        return try {
            httpClient.execute(request).use {
                when (it.statusLine.statusCode) {
                    200 -> {
                        val status = mapper.readValue(it.entity.content, Es2DownloadOrderResponse::class.java)

                        if (status.header.functionExecutionStatus.status != FunctionExecutionStatusType.ExecutedSuccess) {
                            logger.error("SM-DP+ 'order-download' message to service {} for ICCID {} failed with execution status {} (call-id: {}, uri = {})",
                                    config.name,
                                    simEntry.iccid,
                                    status.header.functionExecutionStatus,
                                    header.functionCallIdentifier,
                                    uri)
                            NotUpdatedError("SM-DP+ 'order-download' to ${config.name} failed with status: ${status.header.functionExecutionStatus}")
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
        } catch (e: Exception) {
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

        val header = ES2RequestHeader(
                functionRequesterIdentifier = config.requesterIdentifier,
                functionCallIdentifier = UUID.randomUUID().toString()
        )
        val body = Es2ConfirmOrder(
                header = header,
                eid = eid,
                iccid = simEntry.iccid,
                releaseFlag = true
        )
        val payload = mapper.writeValueAsString(body)

        val request = RequestBuilder.post()
                .setUri("${config.es2plusEndpoint}/gsma/rsp2/es2plus//confirmOrder")
                .setHeader("User-Agent", "gsma-rsp-lpad")
                .setHeader("X-Admin-Protocol", "gsma/rsp/v2.0.0")
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setEntity(StringEntity(payload))
                .build()

        return try {
            httpClient.execute(request).use {
                when (it.statusLine.statusCode) {
                    200 -> {
                        val status = mapper.readValue(it.entity.content, Es2ConfirmOrderResponse::class.java)

                        if (status.header.functionExecutionStatus.status != FunctionExecutionStatusType.ExecutedSuccess) {
                            logger.error("SM-DP+ 'order-confirm' message to service {} for ICCID {} failed with execution status {} (call-id: {})",
                                    config.name,
                                    simEntry.iccid,
                                    status.header.functionExecutionStatus,
                                    header.functionCallIdentifier)
                            NotUpdatedError("SM-DP+ 'order-confirm' to ${config.name} failed with status: ${status.header.functionExecutionStatus}")
                                    .left()
                        } else {
                            // XXX Is just logging good enough?
                            if (status.eid.isNullOrEmpty()) {
                                logger.warn("No EID returned from service {} for ICCID {} for SM-DP+ 'order-confirm' message (call-id: {})",
                                        config.name,
                                        simEntry.iccid,
                                        header.functionCallIdentifier)
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
                                        config.name,
                                        eid,
                                        status.eid,
                                        header.functionCallIdentifier)
                            }
                            logger.info("SM-DP+ 'order-confirm' message to service {} for ICCID {} completed OK (call-id: {})",
                                    config.name,
                                    simEntry.iccid,
                                    header.functionCallIdentifier)

                            val simEntryId = simEntry.id
                            val statusMatchingId = status.matchingId

                            when {
                                simEntryId == null -> AdapterError("simEntryId == null").left()
                                statusMatchingId == null -> AdapterError("statusMatchingId == null").left()
                                else -> dao.setSmDpPlusStateAndMatchingId(simEntryId, SmDpPlusState.RELEASED, statusMatchingId)
                            }
                        }
                    }
                    else -> {
                        logger.error("SM-DP+ 'order-confirm' message to service {} for ICCID {} failed with status code {} (call-id: {})",
                                config.name,
                                simEntry.iccid,
                                it?.statusLine?.statusCode,
                                header.functionCallIdentifier)
                        NotUpdatedError("SM-DP+ 'order-confirm' to ${config.name} failed with code: ${it.statusLine.statusCode}")
                                .left()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("SM-DP+ 'order-confirm' message to service {} for ICCID {} failed with error: {}",
                    config.name,
                    simEntry.iccid,
                    e)
            AdapterError("SM-DP+ 'order-confirm' message to service ${config.name} failed with error: $e")
                    .left()
        }
    }

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



    // XXXX Stop using this abomination!!!! Use the Es2PlusClient instead.
    // This code is buggy and (obviously) untested.

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
                functionRequesterIdentifier = config.requesterIdentifier,
                functionCallIdentifier = UUID.randomUUID().toString()
        )
        val body = Es2PlusProfileStatus(
                header = header,
                iccidList = iccidList.map { IccidListEntry(iccid = it) }
        )
        val payload = mapper.writeValueAsString(body)

        val request = RequestBuilder.post()
                // XXX This is a hack due to previous sloppiness and lack of testing.
                .setUri("${config.es2plusEndpoint}/gsma/rsp2/es2plus/getProfileStatus")
                .setHeader("User-Agent", "gsma-rsp-lpad")
                .setHeader("X-Admin-Protocol", "gsma/rsp/v2.0.0")
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setEntity(StringEntity(payload))
                .build()

        /* Pretty print version of ICCID list. */
        val iccids = iccidList.joinToString(prefix = "[", postfix = "]")

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
                                    header.functionCallIdentifier)
                            NotUpdatedError("SM-DP+ 'profile-status' to ${config.name} failed with status: ${status.header.functionExecutionStatus}")
                                    .left()

                        } else {
                            logger.info("SM-DP+ 'profile-status' message to service {} for ICCID {} completed OK (call-id: {})",
                                    config.name,
                                    iccids,
                                    header.functionCallIdentifier)
                            val profileStatusList = status.profileStatusList

                            if (!profileStatusList.isNullOrEmpty())
                                profileStatusList.right()
                            else
                                NotFoundError("No information found for ICCID $iccids in SM-DP+ 'profile-status' message to service ${config.name}")
                                        .left()
                        }
                    }
                    else -> {
                        logger.error("SM-DP+ 'profile-status' message to service {} for ICCID {} failed with status code {} (call-id: {})",
                                config.name,
                                iccids,
                                it.statusLine.statusCode,
                                header.functionCallIdentifier)
                        NotUpdatedError("SM-DP+ 'order-confirm' to ${config.name} failed with code: ${it.statusLine.statusCode}")
                                .left()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("SM-DP+ 'profile-status' message to service ${config.name} via endpoint '${config.es2plusEndpoint}' for ICCID ${iccids} failed with error.",
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