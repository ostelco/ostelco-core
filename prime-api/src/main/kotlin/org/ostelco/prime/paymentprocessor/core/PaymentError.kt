package org.ostelco.prime.paymentprocessor.core

import org.ostelco.prime.core.ErrorType
import org.ostelco.prime.core.InternalError

sealed class PaymentError(val description: String, var externalErrorMessage : String? = null, errorType: ErrorType) : InternalError(errorType)

class ForbiddenError(description: String, externalErrorMessage: String? = null) : PaymentError(description, externalErrorMessage, ErrorType.CLIENT_ERROR)

class NotFoundError(description: String, externalErrorMessage: String? = null) : PaymentError(description, externalErrorMessage ,ErrorType.CLIENT_ERROR)

class BadGatewayError(description: String, externalErrorMessage: String? = null) : PaymentError(description, externalErrorMessage, ErrorType.SERVER_ERROR)