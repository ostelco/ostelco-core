package org.ostelco.prime.paymentprocessor.core

import org.ostelco.prime.core.InternalError

sealed class PaymentError(val description: String, var externalErrorMessage : String? = null) : InternalError()

class ForbiddenError(description: String, externalErrorMessage: String? = null) : PaymentError(description, externalErrorMessage)

class NotFoundError(description: String, externalErrorMessage: String? = null) : PaymentError(description, externalErrorMessage )

class BadGatewayError(description: String, externalErrorMessage: String? = null) : PaymentError(description, externalErrorMessage)