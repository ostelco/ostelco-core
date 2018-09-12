package org.ostelco.prime.paymentprocessor.core

sealed class PaymentError(val description: String) {
    var externalErrorMessage : String? = null
}

class ForbiddenError(description: String) : PaymentError(description) {
}

class NotFoundError(description: String) : PaymentError(description) {
}

class BadGatewayError(description: String) : PaymentError(description) {
}