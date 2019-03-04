package org.ostelco.simcards.adapter

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
import org.ostelco.prime.storage.NotFoundError
import org.ostelco.prime.storage.NotUpdatedError
import org.ostelco.prime.storage.StoreError
import org.ostelco.sim.es2plus.*
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SmDpPlusState
import java.util.*
import javax.ws.rs.core.MediaType

/**
 * An adapter that can connect to SIM profile vendors and activate
 * the requested SIM profile.
 *
 * Will connect to the SM-DP+  and then activate the profile, so that when
 * user equpiment tries to download a profile, it will get a profile to
 * download.
 */
data class ProfileVendorAdapter (
        @JsonProperty("id") val id: Long,
        @JsonProperty("name") val name: String) {

    private val logger by getLogger()

    /* For payload serializing. */
    private val mapper = jacksonObjectMapper()

    /**
     * Requests the an external Profile Vendor to activate the
     * SIM profile.
     * @param client  HTTP client
     * @param config SIM vendor specific configuration
     * @param dao  DB interface
     * @param eid  ESIM id
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    fun activate(httpClient: CloseableHttpClient,
                 config: ProfileVendorConfig,
                 dao: SimInventoryDAO,
                 eid: String?,
                 simEntry: SimEntry): Either<StoreError, SimEntry> =
            downloadOrder(httpClient, config, dao, simEntry)
                    .flatMap {
                        confirmOrder(httpClient, config, dao, eid, it)
                    }

    /**
     * Initiate activation of a SIM profile with an external Profile Vendor
     * by sending a SM-DP+ 'download-order' message.
     * @param client  HTTP client
     * @param config SIM vendor specific configuration
     * @param dao  DB interface
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    fun downloadOrder(httpClient: CloseableHttpClient,
                      config: ProfileVendorConfig,
                      dao: SimInventoryDAO,
                      simEntry: SimEntry): Either<StoreError, SimEntry> {
        val header = ES2RequestHeader(
                functionRequesterIdentifier = config.requesterIndentifier,
                functionCallIdentifier = "downloadOrder"
        )
        val body = Es2PlusDownloadOrder(
                header = header,
                iccid = simEntry.iccid
        )
        val payload = mapper.writeValueAsString(body)

        val request = RequestBuilder.post()
                .setUri("${config.es2plusEndpoint}/downloadOrder")
                .setHeader("User-Agent", "gsma-rsp-lpad")
                .setHeader("X-Admin-Protocol", "gsma/rsp/v2.0.0")
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setEntity(StringEntity(payload))
                .build()

        return httpClient.execute(request).use {
            when (it.statusLine.statusCode) {
                200 -> {
                    val status = mapper.readValue(it.entity.content, Es2DownloadOrderResponse::class.java)

                    if (status.header.functionExecutionStatus.status != FunctionExecutionStatusType.ExecutedSuccess) {
                        logger.error("SM-DP+ 'order-download' message to service %s for ICCID %s failed with execution status %s (call-id: %s)",
                                config.name,
                                simEntry.iccid,
                                status.header.functionExecutionStatus,
                                header.functionCallIdentifier)
                        NotUpdatedError("sm-dp+", simEntry.iccid).left()
                    } else {
                        logger.info("SM-DP+ 'order-download' message to service {} for ICCID {} completed OK (call-id: {})",
                                config.name,
                                simEntry.iccid,
                                header.functionCallIdentifier)
                        dao.setSmDpPlusState(simEntry.id!!, SmDpPlusState.ALLOCATED)
                    }
                }
                else -> {
                    logger.error("SM-DP+ 'order-download' message to service %s for ICCID %s failed with status code %d (call-id: %s)",
                            config.name,
                            simEntry.iccid,
                            it.statusLine.statusCode,
                            header.functionCallIdentifier)
                    NotUpdatedError("sm-dp+", simEntry.iccid).left()
                }
            }
        }
    }

    /**
     * Complete the activation of a SIM profile with an external Profile Vendor
     * by sending a SM-DP+ 'confirmation' message.
     * @param client  HTTP client
     * @param config SIM vendor specific configuration
     * @param dao  DB interface
     * @param eid  ESIM id
     * @param simEntry  SIM profile to activate
     * @return Updated SIM profile
     */
    fun confirmOrder(httpClient: CloseableHttpClient,
                     config: ProfileVendorConfig,
                     dao: SimInventoryDAO,
                     eid: String? = null,
                     simEntry: SimEntry): Either<StoreError, SimEntry> {
        val header = ES2RequestHeader(
                functionRequesterIdentifier = config.requesterIndentifier,
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
                .setUri("${config.es2plusEndpoint}/confirmOrder")
                .setHeader("User-Agent", "gsma-rsp-lpad")
                .setHeader("X-Admin-Protocol", "gsma/rsp/v2.0.0")
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setEntity(StringEntity(payload))
                .build()

        return httpClient.execute(request).use {
            when (it.statusLine.statusCode) {
                200 -> {
                    val status = mapper.readValue(it.entity.content, Es2ConfirmOrderResponse::class.java)

                    if (status.header.functionExecutionStatus.status != FunctionExecutionStatusType.ExecutedSuccess) {
                        logger.error("SM-DP+ 'order-confirm' message to service %s for ICCID %s failed with execution status %s (call-id: %s)",
                                config.name,
                                simEntry.iccid,
                                status.header.functionExecutionStatus,
                                header.functionCallIdentifier)
                        NotUpdatedError("sm-dp+", simEntry.iccid).left()
                    } else {
                        // XXX Is just logging good enough?
                        if (status.eid.isNullOrEmpty()) {
                            logger.warn("No EID returned from service {} for ICCID {} for SM-DP+ 'order-confirm' message (call-id: {})",
                                    config.name,
                                    simEntry.iccid,
                                    header.functionCallIdentifier)
                        } else {
                            dao.setEidOfSimProfile(simEntry.id!!, status.eid!!)
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
                        dao.setSmDpPlusStateAndMatchingId(simEntry.id!!, SmDpPlusState.RELEASED, status.matchingId!!)
                    }
                }
                else -> {
                    logger.error("SM-DP+ 'order-confirm' message to service %s for ICCID %s failed with status code %d (call-id: %s)",
                            config.name,
                            simEntry.iccid,
                            it.statusLine.statusCode,
                            header.functionCallIdentifier)
                    NotUpdatedError("sm-dp+", simEntry.iccid).left()
                }
            }
        }
    }

    /**
     * Downloads the SM-DP+ 'profile status' information for an ICCID from
     * a SM-DP+ service.
     * @param client  HTTP client
     * @param config  SIM vendor specific configuration
     * @param iccid  ICCID
     * @return SM-DP+ 'profile status' for ICCID
     */
    fun getProfileStatus(httpClient: CloseableHttpClient,
                         config: ProfileVendorConfig,
                         iccid: String): Either<StoreError, ProfileStatus> =
            getProfileStatus(httpClient, config, listOf(iccid))
                    .flatMap {
                        it.first().right()
                    }

    /* XXX Missing:
           1. unit tests
           2. enabled integration test - depends on support in SM-DP+ emulator */

    /**
     * Downloads the SM-DP+ 'profile status' information for a list of ICCIDs
     * from a SM-DP+ service.
     * @param client  HTTP client
     * @param config  SIM vendor specific configuration
     * @param iccidList  list with ICCID
     * @return  A list with SM-DP+ 'profile status' information
     */
    private fun getProfileStatus(httpClient: CloseableHttpClient,
                                 config: ProfileVendorConfig,
                                 iccidList: List<String>): Either<StoreError, List<ProfileStatus>> {
        if (iccidList.isNullOrEmpty()) {
            logger.error("One or more ICCID values required in SM-DP+ 'profile-status' message to service %s",
                    config.name)
            return NotFoundError("", "").left()
        }

        val header = ES2RequestHeader(
                functionRequesterIdentifier = config.requesterIndentifier,
                functionCallIdentifier = UUID.randomUUID().toString()
        )
        val body = Es2PlusProfileStatus(
                header = header,
                iccidList = iccidList.map { IccidListEntry(iccid = it) }
        )
        val payload = mapper.writeValueAsString(body)

        val request = RequestBuilder.post()
                .setUri("${config.es2plusEndpoint}/getProfileStatus")
                .setHeader("User-Agent", "gsma-rsp-lpad")
                .setHeader("X-Admin-Protocol", "gsma/rsp/v2.0.0")
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setEntity(StringEntity(payload))
                .build()

        return httpClient.execute(request).use {
            when (it.statusLine.statusCode) {
                200 -> {
                    val status = mapper.readValue(it.entity.content, Es2ProfileStatusResponse::class.java)

                    if (status.header.functionExecutionStatus.status != FunctionExecutionStatusType.ExecutedSuccess) {

                        logger.error("SM-DP+ 'profile-status' message to service %s for ICCID %s failed with execution status %s (call-id: %s)",
                                config.name,
                                iccidList.joinToString(prefix = "[", postfix = "]"),
                                status.header.functionExecutionStatus,
                                header.functionCallIdentifier)
                        NotUpdatedError("sm-dp+", iccidList.joinToString(prefix = "[", postfix = "]"))
                                .left()

                    } else {
                        logger.info("SM-DP+ 'profile-status' message to service {} for ICCID {} completed OK (call-id: {})",
                                config.name,
                                iccidList.joinToString(prefix = "[", postfix = "]"),
                                header.functionCallIdentifier)
                        val profileStatusList = status.profileStatusList

                        if (!profileStatusList.isNullOrEmpty())
                        profileStatusList.right()
                        else
                            NotFoundError("","").left()
                    }
                }
                else -> {
                    logger.error("SM-DP+ 'profile-status' message to service %s for ICCID %s failed with status code %d (call-id: %s)",
                            config.name,
                            iccidList.joinToString(prefix = "[", postfix = "]"),
                            it.statusLine.statusCode,
                            header.functionCallIdentifier)
                    NotFoundError("", "").left()
                }
            }
        }
    }
}