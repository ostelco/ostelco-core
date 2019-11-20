package org.ostelco.prime.admin.resources

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.module.kotlin.readValue
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.BadRequestError
import org.ostelco.prime.apierror.InternalServerError
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.getLogger
import org.ostelco.prime.jersey.logging.Critical
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.model.IdentityVerification
import org.ostelco.prime.model.JumioScanData
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.ScanResult
import org.ostelco.prime.model.ScanStatus
import org.ostelco.prime.model.Similarity
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource
import java.io.IOException
import java.time.Instant
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response


/**
 * Resource used to handle the eKYC related REST calls.
 */
@Path("/ekyc/callback")
class KYCResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    private fun toRegularMap(m: MultivaluedMap<String, String>?): Map<String, String> {
        val map = HashMap<String, String>()
        if (m == null) {
            return map
        }
        for (entry in m.entries) {
            val sb = StringBuilder()
            for (s in entry.value) {
                if (sb.length > 0) {
                    sb.append(',')
                }
                sb.append(s)
            }
            map[entry.key] = sb.toString()
        }
        return map
    }

    private fun toScanStatus(status: String): ScanStatus {
        return when (status) {
            "SUCCESS" -> ScanStatus.APPROVED
            else -> ScanStatus.REJECTED
        }
    }

    internal fun toIdentityVerification(jsonData: String?): IdentityVerification? {
        try {
            if (jsonData != null) {
                return objectMapper.readValue(jsonData)
            }
        } catch (e: IOException) {
            logger.error("Cannot parse Json Data: $jsonData", e)
        }
        return null
    }

    private fun toScanInformation(dataMap: Map<String, String>): Either<ApiError, ScanInformation> {
        try {
            val vendorScanReference: String = dataMap[JumioScanData.JUMIO_SCAN_ID.s]!!
            var status: ScanStatus = toScanStatus(dataMap[JumioScanData.SCAN_STATUS.s]!!)
            val verificationStatus: String = dataMap[JumioScanData.VERIFICATION_STATUS.s]!!
            val time: Long = Instant.parse(dataMap[JumioScanData.CALLBACK_DATE.s]!!).toEpochMilli()
            val type: String? = dataMap[JumioScanData.ID_TYPE.s]
            val country: String? = dataMap[JumioScanData.ID_COUNTRY.s]
            val firstName: String? = dataMap[JumioScanData.ID_FIRSTNAME.s]
            val lastName: String? = dataMap[JumioScanData.ID_LASTNAME.s]
            val dob: String? = dataMap[JumioScanData.ID_DOB.s]
            val expiry: String? = dataMap[JumioScanData.ID_EXPIRY.s]
            val scanId: String = dataMap[JumioScanData.SCAN_ID.s]!!
            val identityVerificationData: String? = dataMap[JumioScanData.IDENTITY_VERIFICATION.s]
            var rejectReason: IdentityVerification? = null

            // Check if the id matched with the photo.
            if (verificationStatus.toUpperCase() == JumioScanData.APPROVED_VERIFIED.s) {
                val identityVerification = toIdentityVerification(identityVerificationData)
                if (identityVerification == null) {
                    // something gone wrong while parsing identityVerification
                    rejectReason = null
                    status = ScanStatus.REJECTED
                } else {
                    // identityVerification field is present
                    val similarity = identityVerification.similarity
                    val validity = identityVerification.validity
                    if (!(similarity == Similarity.MATCH && validity)) {
                        status = ScanStatus.REJECTED
                        rejectReason = identityVerification
                    }
                }
            }
            val countryCode = getCountryCodeForScan(scanId)
            if (countryCode == null) {
                return NotFoundError("Cannot find country for scan $scanId", ApiErrorCode.FAILED_TO_GET_COUNTRY_FOR_SCAN).left()
            } else {
                return ScanInformation(
                        scanId,
                        countryCode,
                        status,
                        ScanResult(
                                vendorScanReference = vendorScanReference,
                                verificationStatus = verificationStatus,
                                time = time,
                                type = type,
                                country = country,
                                firstName = firstName,
                                lastName = lastName,
                                dob = dob,
                                expiry = expiry,
                                rejectReason = rejectReason
                        )).right()
            }
        } catch (e: NullPointerException) {
            logger.error("Missing mandatory fields in scan result $dataMap", e)
            return BadRequestError("Missing mandatory fields in scan result", ApiErrorCode.FAILED_TO_CONVERT_SCAN_RESULT).left()
        }
    }

    @Critical
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun handleCallback(
            @Context request: HttpServletRequest,
            @Context httpHeaders: HttpHeaders,
            formData: MultivaluedMap<String, String>): Response {
        dumpRequestInfo(request, httpHeaders, formData)
        return toScanInformation(toRegularMap(formData))
                .fold({
                    logger.info("Unable to convert scan information from form data")
                    Response.status(it.status).entity(asJson(it))
                }, { scanInformation ->
                    logger.info("Updating scan information ${scanInformation.scanId} jumioIdScanReference ${scanInformation.scanResult?.vendorScanReference}")
                    updateScanInformation(scanInformation, formData).fold(
                            { Response.status(it.status).entity(asJson(it)) },
                            { Response.status(Response.Status.OK).entity(asJson(scanInformation)) })
                }).build()
    }

    private fun getCountryCodeForScan(scanId: String): String? {
        return try {
            storage.getCountryCodeForScan(scanId).fold({
                logger.error("Failed to get country code for scan $scanId")
                null
            }, {
                it
            })
        } catch (e: Exception) {
            logger.error("Caught error while getting country code for scan $scanId")
            return null
        }
    }

    private fun updateScanInformation(scanInformation: ScanInformation, formData: MultivaluedMap<String, String>): Either<ApiError, Unit> {
        return try {
            return storage.updateScanInformation(scanInformation, formData).mapLeft {
                logger.error("Failed to update scan information ${scanInformation.scanId} jumioIdScanReference ${scanInformation.scanResult?.vendorScanReference}")
                NotFoundError("Failed to update scan information. ${it.message}", ApiErrorCode.FAILED_TO_UPDATE_SCAN_RESULTS)
            }
        } catch (e: Exception) {
            logger.error("Caught error while updating scan information ${scanInformation.scanId} jumioIdScanReference ${scanInformation.scanResult?.vendorScanReference}", e)
            Either.left(InternalServerError("Failed to update scan information", ApiErrorCode.FAILED_TO_UPDATE_SCAN_RESULTS))
        }
    }

    //TODO: Prasanth, remove this method after testing
    private fun dumpRequestInfo(request: HttpServletRequest, httpHeaders: HttpHeaders, formData: MultivaluedMap<String, String>): String {
        var result = ""
//        result += "Address: ${request.remoteHost} (${request.remoteAddr} : ${request.remotePort}) \n"
//        result += "Query: ${request.queryString} \n"
//        result += "Headers == > \n"
//        val requestHeaders = httpHeaders.getRequestHeaders()
//        for (entry in requestHeaders.entries) {
//            result += "${entry.key} = ${entry.value}\n"
//        }
        result += "\nRequest Data: \n"
        for (entry in formData.entries) {
            result += "${entry.key} = ${entry.value}\n"
        }
        logger.info(result)

        return result
    }
}
