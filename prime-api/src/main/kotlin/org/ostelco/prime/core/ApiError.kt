package org.ostelco.prime.core

import javax.ws.rs.core.Response

open class ApiError(val description: String) {
    open var status : Response.Status = Response.Status.OK
}

class NotFoundError(description: String) : ApiError(description) {
    override var status : Response.Status = Response.Status.NOT_FOUND
}

class BadGatewayError(description: String) : ApiError(description) {
    override var status : Response.Status = Response.Status.BAD_GATEWAY
}
