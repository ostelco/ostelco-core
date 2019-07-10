package org.ostelco.prime.admin.resources

import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorMapper
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.storage.AdminDataSource
import org.ostelco.prime.storage.StoreError
import java.time.Instant
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/payment")
@Produces(MediaType.APPLICATION_JSON)
class PaymentTransactionResource {

    private val storage by lazy { getResource<AdminDataSource>() }

    @GET
    @Path("/transactions")
    fun fetchPaymentTransactions(@QueryParam("start")
                                 @DefaultValue("-1")   /* A bit cheap, but works. */
                                 start: Long,
                                 @QueryParam("end")
                                 @DefaultValue("-1")
                                 end: Long): Response =
            storage.getPaymentTransactions(
                    start = ofEpochSecondToMilli(start, getEpochSeconds(A_DAY_AGO)),
                    end = ofEpochSecondToMilli(end, getEpochSeconds()))
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_FETCH_PAYMENT_TRANSACTIONS) },
                            { ok(mapOf("payments" to it)) }
                    ).build()

    @GET
    @Path("/purchases")
    fun fetchPurchaseTransactions(@QueryParam("start")
                                  @DefaultValue("-1")
                                  start: Long,
                                  @QueryParam("end")
                                  @DefaultValue("-1")
                                  end: Long): Response =
            storage.getPurchaseTransactions(
                    start = ofEpochSecondToMilli(start, getEpochSeconds(A_DAY_AGO)),
                    end = ofEpochSecondToMilli(end, getEpochSeconds()))
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_FETCH_PURCHASE_RECORDS) },
                            { ok(mapOf("purchases" to it)) }
                    ).build()

    @GET
    @Path("/check")
    fun checkTransactions(@QueryParam("start")
                          @DefaultValue("-1")
                          start: Long,
                          @QueryParam("end")
                          @DefaultValue("-1")
                          end: Long): Response =
            storage.checkPaymentTransactions(
                    start = ofEpochSecondToMilli(start, getEpochSeconds(A_DAY_AGO)),
                    end = ofEpochSecondToMilli(end, getEpochSeconds()))
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_PAYMENT_TRANSACTIONS) },
                            { ok(mapOf("mismatch" to it)) }
                    ).build()

    /* Epoch timestamp in milli, now or offset by +/- seconds. */
    private fun getEpochSeconds(offset: Long = 0L): Long = Instant.now().plusSeconds(offset).toEpochMilli().div(1000L)

    /* Seconds to milli with fallback on a default value. */
    private fun ofEpochSecondToMilli(ts: Long, default: Long): Long = ofEpochSecondToMilli(if (ts < 0L) default else ts)

    /* Seconds to milli. */
    private fun ofEpochSecondToMilli(ts: Long): Long = Instant.ofEpochSecond(if (ts < 0L) 0L else ts).toEpochMilli()

    private fun ok(value: Map<String, Any>) =
            Response.status(Response.Status.OK).entity(asJson(value))

    private fun failed(error: PaymentError, code: ApiErrorCode): Response.ResponseBuilder =
            ApiErrorMapper.mapPaymentErrorToApiError(description = error.description,
                    errorCode = code,
                    paymentError = error).let {
                Response.status(it.status).entity(asJson(it))
            }

    private fun failed(error: StoreError, code: ApiErrorCode): Response.ResponseBuilder =
            ApiErrorMapper.mapStorageErrorToApiError(description = error.message,
                    errorCode = code,
                    storeError = error).let {
                Response.status(it.status).entity(asJson(it))
            }

    companion object {
        /* 24 hours ago in seconds. */
        val A_DAY_AGO: Long = -86400L
    }
}
