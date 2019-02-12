package org.ostelco.prime.admin.api

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.*
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.*
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource
import java.net.URLDecoder
import java.time.Instant
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.*
import java.io.IOException




//TODO: Prasanth, Remove after testing
/**
 * Resource used to handle bundles related REST calls.
 */
@Path("/new-ekyc-scanId")
class KYCTestHelperResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    @GET
    @Path("{email}")
    @Produces(MediaType.APPLICATION_JSON)
    fun newEKYCScanId(@Auth token: AccessTokenPrincipal?,
                      @PathParam("email")
                      email: String
                      ): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        val decodedEmail = URLDecoder.decode(email, "UTF-8")
        logger.info("${token.name} Generate new ScanId for $decodedEmail")

        return newEKYCScanId(subscriberId = decodedEmail).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { scanInformation -> Response.status(Response.Status.OK).entity(scanInformation) })
                .build()
    }

    private fun newEKYCScanId(subscriberId: String): Either<ApiError, ScanInformation> {
        return storage.newEKYCScanId(subscriberId)
                .mapLeft { ApiErrorMapper.mapStorageErrorToApiError("Failed to create new scanId", ApiErrorCode.FAILED_TO_CREATE_SCANID, it) }
    }
}

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

    private fun toScanStatus(status: String): ScanStatus  {
        return when (status) {
            "SUCCESS" -> { ScanStatus.APPROVED }
            else -> { ScanStatus.REJECTED }
        }
    }

    private fun toRegularMap(jsonData: String?): Map<String, String>? {
        try {
            if (jsonData != null) {
                return ObjectMapper().readValue(jsonData)
            }
        } catch (e: IOException) {
            logger.error("Cannot parse Json Data: $jsonData")
        }
        return null
    }

    private fun toScanInformation(dataMap: Map<String, String>): ScanInformation? {
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
            var rejectReason: String? = dataMap[JumioScanData.REJECT_REASON.s]
            val scanId: String = dataMap[JumioScanData.SCAN_ID.s]!!
            val identityVerificationData: String? = dataMap[JumioScanData.IDENTITY_VERIFICATION.s]

            // Check if the id matched with the photo.
            if (verificationStatus.toUpperCase() == JumioScanData.APPROVED_VERIFIED.s) {
                val identityVerification = toRegularMap(identityVerificationData)
                if (identityVerification == null) {
                    // something gone wrong while parsing identityVerification
                    rejectReason = """{ "message": "Missing ${JumioScanData.IDENTITY_VERIFICATION.s} information" }"""
                    status = ScanStatus.REJECTED
                } else {
                    // identityVerification field is present
                    val similarity = identityVerification[JumioScanData.SIMILARITY.s]
                    val validity = identityVerification[JumioScanData.VALIDITY.s]
                    if (!(similarity != null && similarity.toUpperCase() == JumioScanData.MATCH.s &&
                            validity != null && validity.toUpperCase() == JumioScanData.TRUE.s)) {
                        status = ScanStatus.REJECTED
                        rejectReason = identityVerificationData
                    }
                }
            }

            return ScanInformation(scanId, status, ScanResult(
                    vendorScanReference = vendorScanReference,
                    verificationStatus = verificationStatus,
                    time = time,
                    type = type,
                    country = country,
                    firstName = firstName,
                    lastName = lastName,
                    dob = dob,
                    rejectReason = rejectReason
            ))
        }
        catch (e: NullPointerException) {
            logger.error("Missing mandatory fields in scan result ${dataMap}")
            return null
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun handleCallback(
            @Context request: HttpServletRequest,
            @Context httpHeaders: HttpHeaders,
            formData: MultivaluedMap<String, String>): Response {
        dumpRequestInfo(request, httpHeaders, formData)
        val scanInformation = toScanInformation(toRegularMap(formData))
        if (scanInformation == null) {
            logger.info("Unable to convert scan information from form data")
            val reqError = BadRequestError("Missing mandatory fields in scan result", ApiErrorCode.FAILED_TO_UPDATE_SCAN_RESULTS)
            return Response.status(reqError.status).entity(asJson(reqError)).build()
        }
        logger.info("Updating scan information ${scanInformation.scanId} jumioIdScanReference ${scanInformation.scanResult?.vendorScanReference}")
        return updateScanInformation(scanInformation, formData).fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(scanInformation)) }).build()
    }

    private fun updateScanInformation(scanInformation: ScanInformation, formData: MultivaluedMap<String, String>): Either<ApiError, Unit> {
        return try {
            return storage.updateScanInformation(scanInformation, formData).mapLeft {
                logger.error("Failed to update scan information ${scanInformation.scanId} jumioIdScanReference ${scanInformation.scanResult?.vendorScanReference}")
                NotFoundError("Failed to update scan information. ${it.message}", ApiErrorCode.FAILED_TO_UPDATE_SCAN_RESULTS)
            }
        } catch (e: Exception) {
            logger.error("Caught error while updating scan information ${scanInformation.scanId} jumioIdScanReference ${scanInformation.scanResult?.vendorScanReference}", e)
            Either.left(BadGatewayError("Failed to update scan information", ApiErrorCode.FAILED_TO_UPDATE_SCAN_RESULTS))
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
        logger.info("$result")

        return result
    }
}
