package org.ostelco.simcards.hss


import arrow.core.Either
import arrow.core.Left
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.getLogger
import org.ostelco.prime.simmanager.AdapterError
import org.ostelco.prime.simmanager.NotUpdatedError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.admin.HssConfig
import javax.ws.rs.core.MediaType

/**
 * This is a datum that is stored in a database.
 *
 * When a VLR asks the HLR for the an authentication triplet, then the
 * HLR will know that it should give an answer.
 *
 * id - is a database internal identifier.
 * name - is an unique instance of  HLR reference.
 */
data class HssEntry(
        @JsonProperty("id") val id: Long,
        @JsonProperty("name") val name: String)

// TODO:  Separate these, they shouldn't be in the same file!

class SimpleHssDispatcher(val name: String,
                          val httpClient: CloseableHttpClient,
                          val config: HssConfig) : HssDispatcher {

    private val logger by getLogger()

    /* For payload serializing. */
    private val mapper = jacksonObjectMapper()

    override fun name() = name

    override fun iAmHealthy(): Boolean = true

    /**
     * Requests the external HLR service to activate the SIM profile.
     * @param simEntry  SIM profile to activate
     * @return Updated SIM Entry
     */

    override fun activate(hssName: String, iccid: String, msisdn: String): Either<SimManagerError, Unit> {
        if (hssName != name) {
            return Left(AdapterError("Attempt to activate  hssName=$hssName, iccid=$iccid, msisdn=$msisdn in a dispatcher for hss named $name"))
        }

        if (!iccid.matches(Regex("^\\d{19,20}"))) {
            return Left(AdapterError("Ill formatted ICCID $iccid"))
        }

        if (iccid.isEmpty()) {
            return NotUpdatedError("Empty ICCID value in SIM activation request to BSSID ${config.name}")
                    .left()
        }

        val body = mapOf(
                "bssid" to config.name,
                "iccid" to iccid,
                "msisdn" to msisdn,
                "userid" to config.userId
        )

        val payload = mapper.writeValueAsString(body)

        val request = RequestBuilder.post()
                .setUri("${config.endpoint}/activate")
                .setHeader("x-api-key", config.apiKey)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setEntity(StringEntity(payload))
                .build()

        return try {
            httpClient.execute(request).use {
                when (it.statusLine.statusCode) {
                    201 -> {
                        logger.info("HLR activation message to BSSID {} for ICCID {} completed OK",
                                config.name,
                                iccid).right()
                    }
                    else -> {
                        logger.warn("HLR activation message to BSSID {} for ICCID {} failed with status ({}) {}",
                                config.name,
                                iccid,
                                it.statusLine.statusCode,
                                it.statusLine.reasonPhrase)
                        NotUpdatedError("Failed to activate ICCID ${iccid} with BSSID ${config.name} (status-code: ${it.statusLine.statusCode})")
                                .left()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("HLR activation message to BSSID {} for ICCID {} failed with error: {}",
                    config.name,
                    iccid,
                    e)
            AdapterError("HLR activation message to BSSID ${config.name} for ICCID ${iccid} failed with error: ${e}")
                    .left()
        }
    }

    /**
     * Requests the external HLR service to deactivate the SIM profile.
     * @param simEntry  SIM profile to deactivate
     * @return Updated SIM profile
     */
    override fun suspend(hssName: String, iccid: String): Either<SimManagerError, Unit> {

        if (hssName != name) {
            return Left(AdapterError("Attempt to suspend  hssName=$hssName, iccid=$iccid in a dispatcher for hss named $name"))
        }

        if (iccid.isEmpty()) {
            return NotUpdatedError("Illegal parameter in SIM deactivation request to BSSID ${config.name}")
                    .left()
        }

        val request = RequestBuilder.delete()
                .setUri("${config.endpoint}/deactivate/${iccid}")
                .setHeader("x-api-key", config.apiKey)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .build()

        return try {
            httpClient.execute(request).use {
                when (it.statusLine.statusCode) {
                    200 -> {
                        logger.info("HLR deactivation message to BSSID {} for ICCID {} completed OK",
                                config.name,
                                iccid).right()
                    }
                    else -> {
                        logger.warn("HLR deactivation message to BSSID {} for ICCID {} failed with status ({}) {}",
                                config.name,
                                iccid,
                                it.statusLine.statusCode,
                                it.statusLine.reasonPhrase)
                        NotUpdatedError("Failed to deactivate ICCID ${iccid} with BSSID ${config.name} (status-code: ${it.statusLine.statusCode}")
                                .left()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("HLR deactivation message to BSSID {} for ICCID {} failed with error: {}",
                    config.name,
                    iccid,
                    e)
            AdapterError("HLR deactivation message to BSSID ${config.name} for ICCID ${iccid} failed with error: ${e}")
                    .left()
        }
    }
}
