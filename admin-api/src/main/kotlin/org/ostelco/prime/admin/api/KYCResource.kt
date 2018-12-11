package org.ostelco.prime.admin.api

import arrow.core.Either
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.BadGatewayError
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.ScanResult
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.storage.AdminDataSource
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.*
import java.util.HashMap
import java.text.SimpleDateFormat


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

    private fun toTimestamp(dateString: String, format: String = "YYYY-MM-DDThh:mm:ss.SSSZ"): Long {
        val df = SimpleDateFormat(format)
        val date = df.parse(dateString)
        return date.time
    }

    private fun toScanInformation(dataMap: Map<String, String>): ScanInformation {
        val vendorScanReference: String = dataMap["jumioIdScanReference"]!!
        val status: String = dataMap["idScanStatus"]!!
        val verificationStatus: String = dataMap["verificationStatus"]!!
        val time: Long = toTimestamp(dataMap["callbackDate"]!!)
        val type: String? = dataMap["idType"]
        val country: String? = dataMap["idCountry"]
        val firstName: String? = dataMap["idFirstName"]
        val lastName: String? = dataMap["idLastName"]
        val dob: String? = dataMap["idDob"]
        val rejectReason: String? = dataMap["rejectReason"]
        val scanId: String = dataMap["merchantIdScanReference"]!!

        return ScanInformation(scanId, ScanResult(
                vendorScanReference = vendorScanReference,
                status = status,
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

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun handleCallback(
            @Context request: HttpServletRequest,
            @Context httpHeaders: HttpHeaders,
            formData: MultivaluedMap<String, String>): Response {

        val scanInformation = toScanInformation(toRegularMap(formData))
        dumpRequestInfo(request, httpHeaders, formData)
        logger.error("Updating scan information ${scanInformation.scanId} jumioIdScanReference ${scanInformation.scanResult?.vendorScanReference}")
        return updateScanInformation(scanInformation).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                {
                    Response.status(Response.Status.OK).entity(asJson(it))
                })
                .build()
    }

    private fun updateScanInformation(scanInformation: ScanInformation): Either<ApiError, Unit> {
        return try {
            return storage.updateScanInformation(scanInformation).mapLeft {
                logger.error("Failed to update scan information ${scanInformation.scanId} jumioIdScanReference ${scanInformation.scanResult?.vendorScanReference}")
                NotFoundError("Failed to update scan information. ${it.message}", ApiErrorCode.FAILED_TO_UPDATE_SCAN_RESULTS)
            }
        } catch (e: Exception) {
            logger.error("Caught error while updating scan information ${scanInformation.scanId} jumioIdScanReference ${scanInformation.scanResult?.vendorScanReference}", e)
            Either.left(BadGatewayError("Failed to update scan information", ApiErrorCode.FAILED_TO_UPDATE_SCAN_RESULTS))
        }
    }

    private fun dumpRequestInfo(request: HttpServletRequest, httpHeaders: HttpHeaders, formData: MultivaluedMap<String, String>): String {
        var result = ""
        result += "Address: ${request.remoteHost} (${request.remoteAddr} : ${request.remotePort}) \n"
        result += "Query: ${request.queryString} \n"
        result += "Headers == > \n"
        val requestHeaders = httpHeaders.getRequestHeaders()
        for (entry in requestHeaders.entries) {
            result += "${entry.key} = ${entry.value}\n"
        }
        result += "Data: \n"
        for (entry in formData.entries) {
            result += "${entry.key} = ${entry.value}\n"
        }
        logger.info("$result")

        return result
    }
}
