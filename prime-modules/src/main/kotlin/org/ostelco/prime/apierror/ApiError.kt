package org.ostelco.prime.apierror

import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.prime.storage.StoreError
import javax.ws.rs.core.Response

sealed class ApiError(val message: String, val errorCode: ApiErrorCode, val error: InternalError?) {
    open var status : Int = 0
}

class InternalServerError(description: String, errorCode: ApiErrorCode, error: InternalError? = null) : ApiError(description, errorCode, error) {
    override var status : Int = Response.Status.INTERNAL_SERVER_ERROR.statusCode
}

class BadRequestError(description: String, errorCode: ApiErrorCode, error: InternalError? = null) : ApiError(description, errorCode, error) {
    override var status : Int = Response.Status.BAD_REQUEST.statusCode
}

class ForbiddenError(description: String, errorCode: ApiErrorCode, error: InternalError? = null) : ApiError(description, errorCode, error) {
    override var status : Int = Response.Status.FORBIDDEN.statusCode
}

class NotFoundError(description: String, errorCode: ApiErrorCode, error: InternalError? = null) : ApiError(description, errorCode, error) {
    override var status : Int = Response.Status.NOT_FOUND.statusCode
}

object ApiErrorMapper {

    val logger by getLogger()

    fun mapPaymentErrorToApiError(description: String, errorCode: ApiErrorCode, paymentError: PaymentError) : ApiError {
        logger.error("{}: {}, paymentError: {}", errorCode, description, asJson(paymentError))
        return when(paymentError) {
            is org.ostelco.prime.paymentprocessor.core.PlanAlredyPurchasedError -> ForbiddenError(description, errorCode, paymentError)
            is org.ostelco.prime.paymentprocessor.core.ForbiddenError  ->  ForbiddenError(description, errorCode, paymentError)
            // FIXME vihang: remove PaymentError from BadGatewayError
            is org.ostelco.prime.paymentprocessor.core.BadGatewayError -> InternalServerError(description, errorCode, paymentError)
            is org.ostelco.prime.paymentprocessor.core.NotFoundError -> NotFoundError(description, errorCode, paymentError)
            is org.ostelco.prime.paymentprocessor.core.PaymentConfigurationError -> InternalServerError(description, errorCode, paymentError)
        }
    }

    fun mapStorageErrorToApiError(description: String, errorCode: ApiErrorCode, storeError: StoreError) : ApiError {
        logger.error("{}: {}, storeError: {}", errorCode, description, asJson(storeError))
        return when(storeError) {
            is org.ostelco.prime.storage.NotFoundError  ->  NotFoundError(description, errorCode, storeError)
            is org.ostelco.prime.storage.AlreadyExistsError  ->  ForbiddenError(description, errorCode, storeError)
            // FIXME vihang: remove StoreError from BadGatewayError
            is org.ostelco.prime.storage.NotCreatedError  ->  InternalServerError(description, errorCode)
            is org.ostelco.prime.storage.NotUpdatedError  ->  InternalServerError(description, errorCode)
            is org.ostelco.prime.storage.NotDeletedError  ->  InternalServerError(description, errorCode)
            is org.ostelco.prime.storage.PartiallyNotDeletedError  ->  InternalServerError(description, errorCode)
            is org.ostelco.prime.storage.ValidationError  ->  ForbiddenError(description, errorCode, storeError)
            is org.ostelco.prime.storage.FileDownloadError  ->  InternalServerError(description, errorCode)
            is org.ostelco.prime.storage.FileDeleteError  ->  InternalServerError(description, errorCode)
            is org.ostelco.prime.storage.SystemError  ->  InternalServerError(description, errorCode)
            is org.ostelco.prime.storage.DatabaseError  ->  InternalServerError(description, errorCode)
        }
    }

    fun mapSimManagerErrorToApiError(description: String, errorCode: ApiErrorCode, simManagerError: SimManagerError) : ApiError {
        logger.error("{}: {}, simManagerError: {}", errorCode, description, asJson(simManagerError))
        return when (simManagerError) {
            is org.ostelco.prime.simmanager.NotFoundError -> NotFoundError(description, errorCode, simManagerError)
            is org.ostelco.prime.simmanager.NotUpdatedError -> BadRequestError(description, errorCode, simManagerError)
            is org.ostelco.prime.simmanager.ForbiddenError -> ForbiddenError(description, errorCode, simManagerError)
            is org.ostelco.prime.simmanager.AdapterError -> InternalServerError(description, errorCode, simManagerError)
            is org.ostelco.prime.simmanager.DatabaseError -> InternalServerError(description, errorCode, simManagerError)
            is org.ostelco.prime.simmanager.SystemError -> InternalServerError(description, errorCode, simManagerError)
        }
    }
}
