package org.ostelco.prime.core

import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.storage.StoreError
import javax.ws.rs.core.Response

sealed class ApiError(val message: String, val errorCode: ApiErrorCode, val error: InternalError?) {
    open var status : Int = 0
}

class BadGatewayError(description: String, errorCode: ApiErrorCode, error: InternalError? = null) : ApiError(description, errorCode, error) {
    override var status : Int = Response.Status.BAD_GATEWAY.getStatusCode()
}

class BadRequestError(description: String, errorCode: ApiErrorCode, error: InternalError? = null) : ApiError(description, errorCode, error) {
    override var status : Int = Response.Status.BAD_REQUEST.getStatusCode()
}

class ForbiddenError(description: String, errorCode: ApiErrorCode, error: InternalError? = null) : ApiError(description, errorCode, error) {
    override var status : Int = Response.Status.FORBIDDEN.getStatusCode()
}

class InsuffientStorageError(description: String, errorCode: ApiErrorCode, error: InternalError? = null) : ApiError(description, errorCode, error) {
    override var status : Int = 507
}

class NotFoundError(description: String, errorCode: ApiErrorCode, error: InternalError? = null) : ApiError(description, errorCode, error) {
    override var status : Int = Response.Status.NOT_FOUND.getStatusCode()
}

fun mapPaymentErrorToApiError(description: String, errorCode: ApiErrorCode, paymentError: PaymentError) : ApiError {
    return when(paymentError) {
        is org.ostelco.prime.paymentprocessor.core.ForbiddenError  ->  org.ostelco.prime.core.ForbiddenError(description, errorCode, paymentError)
        is org.ostelco.prime.paymentprocessor.core.BadGatewayError -> org.ostelco.prime.core.BadGatewayError(description, errorCode)
        is org.ostelco.prime.paymentprocessor.core.NotFoundError -> org.ostelco.prime.core.NotFoundError(description, errorCode, paymentError)
    }
}

fun mapStorageErrorToApiError(description: String, errorCode: ApiErrorCode, storeError: StoreError) : ApiError {
    return when(storeError) {
        is org.ostelco.prime.storage.NotFoundError  ->  org.ostelco.prime.core.NotFoundError(description, errorCode, storeError)
        is org.ostelco.prime.storage.AlreadyExistsError  ->  org.ostelco.prime.core.ForbiddenError(description, errorCode, storeError)
        is org.ostelco.prime.storage.NotCreatedError  ->  org.ostelco.prime.core.BadGatewayError(description, errorCode)
        is org.ostelco.prime.storage.NotUpdatedError  ->  org.ostelco.prime.core.BadGatewayError(description, errorCode)
        is org.ostelco.prime.storage.NotDeletedError  ->  org.ostelco.prime.core.BadGatewayError(description, errorCode)
        is org.ostelco.prime.storage.ValidationError  ->  org.ostelco.prime.core.ForbiddenError(description, errorCode, storeError)
    }
}