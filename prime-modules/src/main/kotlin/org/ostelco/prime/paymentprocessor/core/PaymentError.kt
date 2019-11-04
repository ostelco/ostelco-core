package org.ostelco.prime.paymentprocessor.core

import org.ostelco.prime.apierror.InternalError

sealed class PaymentError(val description: String, var message : String? = null, val error: InternalError?) : InternalError()

class PlanAlredyPurchasedError(description: String, message: String? = null, error: InternalError? = null) : PaymentError(description, message, error)

class PaymentFailedError(description: String, message: String? = null, error: InternalError? = null) : PaymentError(description, message, error)

class ForbiddenError(description: String, message: String? = null, error: InternalError? = null) : PaymentError(description, message, error)

class NotFoundError(description: String, message: String? = null, error: InternalError? = null) : PaymentError(description, message, error)

class BadGatewayError(description: String, message: String? = null, error: InternalError? = null) : PaymentError(description, message, error)

class PaymentConfigurationError(description: String, message: String? = null, error: InternalError? = null) : PaymentError(description, message, error)
