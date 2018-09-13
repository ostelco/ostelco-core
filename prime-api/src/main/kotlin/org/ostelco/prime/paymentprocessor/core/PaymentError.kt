package org.ostelco.prime.paymentprocessor.core

import org.ostelco.prime.core.ErrorType
import org.ostelco.prime.core.InternalError

sealed class PaymentError(val description: String, errorType: ErrorType) : InternalError(errorType) {
    var externalErrorMessage : String? = null
}

class ForbiddenError(description: String) : PaymentError(description, ErrorType.CLIENT_ERROR)

class NotFoundError(description: String) : PaymentError(description, ErrorType.CLIENT_ERROR)

class BadGatewayError(description: String) : PaymentError(description, ErrorType.SERVER_ERROR)