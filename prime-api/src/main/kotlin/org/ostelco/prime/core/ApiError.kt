package org.ostelco.prime.core

import javax.ws.rs.core.Response

sealed class ApiError(val description: String) {
    open var status : Int = 0
}

class BadGatewayError(description: String) : ApiError(description) {
    override var status : Int = Response.Status.BAD_GATEWAY.getStatusCode()
}

class BadRequestError(description: String) : ApiError(description) {
    override var status : Int = Response.Status.BAD_REQUEST.getStatusCode()
}

class ForbiddenError(description: String) : ApiError(description) {
    override var status : Int = Response.Status.FORBIDDEN.getStatusCode()
}

class InsuffientStorageError(description: String) : ApiError(description) {
    override var status : Int = 507
}

class NotFoundError(description: String) : ApiError(description) {
    override var status : Int = Response.Status.NOT_FOUND.getStatusCode()
}
