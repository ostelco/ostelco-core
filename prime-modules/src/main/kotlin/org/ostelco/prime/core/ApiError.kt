package org.ostelco.prime.core

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
