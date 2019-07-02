package org.ostelco.prime.admin.resources

import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorMapper
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.storage.AdminDataSource
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

@Path("/payments")
class PaymentTransactionResource {

    private val storage by lazy { getResource<AdminDataSource>() }

    @GET
    @Path("stripe/transactions")
    fun fetchStripeTransactions(@QueryParam("after")
                                after: Long = epoch(-86400),
                                @QueryParam("before")
                                before: Long = 0L): Response =
            storage.getPaymentTransactions(after, before)
                    .fold(
                            { failed(it, ApiErrorCode.FAILED_TO_FETCH_PAYMENT_TRANSACTIONS) },
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
}
