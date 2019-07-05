package org.ostelco.prime.admin.resources

import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorMapper
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.storage.AdminDataSource
import org.ostelco.prime.storage.StoreError
import java.time.Instant
import javax.validation.constraints.Pattern
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

@Path("/payment")
class PaymentTransactionResource {

    private val storage by lazy { getResource<AdminDataSource>() }

    @GET
    @Path("/transactions")
    fun fetchPaymentTransactions(@QueryParam("start")
                                 @Pattern(regexp = DIGITS_ONLY)
                                 start: Long = getEpochMilli(A_DAY_AGO),
                                 @QueryParam("end")
                                 @Pattern(regexp = DIGITS_ONLY)
                                 end: Long = getEpochMilli()): Response =
            storage.getPaymentTransactions(ofEpochSecondToMilli(start), ofEpochSecondToMilli(end))
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_FETCH_PAYMENT_TRANSACTIONS) },
                            { ok(it) }
                    ).build()

    @GET
    @Path("/purchases")
    fun fetchPurchaseTransactions(@QueryParam("start")
                                  @Pattern(regexp = DIGITS_ONLY)
                                  start: Long = getEpochMilli(A_DAY_AGO),
                                  @QueryParam("end")
                                  @Pattern(regexp = DIGITS_ONLY)
                                  end: Long = getEpochMilli()): Response =
            storage.getPurchaseTransactions(ofEpochSecondToMilli(start), ofEpochSecondToMilli(end))
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_FETCH_PURCHASE_TRANSACTIONS) },
                            { ok(it) }
                    ).build()

    @GET
    @Path("/check")
    fun checkTransactions(@QueryParam("start")
                          @Pattern(regexp = DIGITS_ONLY)
                          start: Long = getEpochMilli(A_DAY_AGO),
                          @QueryParam("end")
                          @Pattern(regexp = DIGITS_ONLY)
                          end: Long = getEpochMilli()): Response =
            storage.checkPaymentTransactions(ofEpochSecondToMilli(start), ofEpochSecondToMilli(end))
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_PAYMENT_TRANSACTIONS) },
                            { ok(it) }
                    ).build()

    /* Epoch timestamp in milli, now or offset by +/- seconds. */
    private fun getEpochMilli(offset: Long = 0L): Long = Instant.now().plusSeconds(offset).toEpochMilli()

    /* Internally all timestamps are in milliseconds. */
    private fun ofEpochSecondToMilli(ts: Long): Long = Instant.ofEpochSecond(ts).toEpochMilli()

    companion object {
        /* 24 hours ago in seconds. */
        val A_DAY_AGO: Long = -86400L

        /* Regexp maching digits only. */
        const val DIGITS_ONLY = "^(0|[1-9][0-9]*)$"
    }

    private fun ok(value: List<Any>) =
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
}
