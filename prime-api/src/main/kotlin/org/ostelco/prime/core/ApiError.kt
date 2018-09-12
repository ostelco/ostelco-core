package org.ostelco.prime.core

import javax.ws.rs.core.Response

sealed class ApiError(val description: String, val errorCode: ApiErrorCode) {
    open var status : Int = 0
}

class BadGatewayError(description: String, errorCode: ApiErrorCode) : ApiError(description, errorCode) {
    override var status : Int = Response.Status.BAD_GATEWAY.getStatusCode()
}

class BadRequestError(description: String, errorCode: ApiErrorCode) : ApiError(description, errorCode) {
    override var status : Int = Response.Status.BAD_REQUEST.getStatusCode()
}

class ForbiddenError(description: String, errorCode: ApiErrorCode) : ApiError(description, errorCode) {
    override var status : Int = Response.Status.FORBIDDEN.getStatusCode()
}

class InsuffientStorageError(description: String, errorCode: ApiErrorCode) : ApiError(description, errorCode) {
    override var status : Int = 507
}

class NotFoundError(description: String, errorCode: ApiErrorCode) : ApiError(description, errorCode) {
    override var status : Int = Response.Status.NOT_FOUND.getStatusCode()
}
