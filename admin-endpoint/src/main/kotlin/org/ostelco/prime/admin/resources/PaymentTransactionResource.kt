package org.ostelco.prime.admin.resources

import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorMapper
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.storage.AdminDataSource
import org.ostelco.prime.storage.StoreError
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

@Path("/payment")
class PaymentTransactionResource {

    private val storage by lazy { getResource<AdminDataSource>() }

    @GET
    @Path("/transactions")
    fun fetchPaymentTransactions(@QueryParam("after")
                                 after: Long = epoch(A_DAY_AGO),
                                 @QueryParam("before")
                                 before: Long = epoch()): Response =
            storage.getPaymentTransactions(after, before)
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_FETCH_PAYMENT_TRANSACTIONS) },
                            { ok(it) }
                    ).build()

    @GET
    @Path("/purchases")
    fun fetchPurchaseTransactions(@QueryParam("after")
                                  after: Long = epoch(A_DAY_AGO),
                                  @QueryParam("before")
                                  before: Long = epoch()): Response =
            storage.getPurchaseTransactions(after, before)
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_FETCH_PURCHASE_TRANSACTIONS) },
                            { ok(it) }
                    ).build()

    @GET
    @Path("/check/transaction")
    fun checkTransactions(@QueryParam("after")
                          after: Long = epoch(A_DAY_AGO),
                          @QueryParam("before")
                          before: Long = epoch()): Response =
            storage.checkPaymentTransactions(after, before)
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_CHECK_PAYMENT_TRANSACTIONS) },
                            { ok(it) }
                    ).build()

    /* Epoch timestamp, now or offset by +/- seconds. */
    private fun epoch(offset: Long = 0L): Long =
            LocalDateTime.now(ZoneOffset.UTC)
                    .plusSeconds(offset)
                    .atZone(ZoneOffset.UTC).toEpochSecond()

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

    companion object {
        /* 24 hours ago in seconds. */
        val A_DAY_AGO: Long = -86400
    }
}
