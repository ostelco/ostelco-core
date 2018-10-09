package org.ostelco.prime.imei.core

import org.ostelco.prime.apierror.InternalError

sealed class ImeiLookupError(val description: String, var externalErrorMessage : String? = null) : InternalError()

class ImeiNotFoundError(description: String, externalErrorMessage: String? = null) : ImeiLookupError(description, externalErrorMessage )

class BadRequestError(description: String, externalErrorMessage: String? = null) : ImeiLookupError(description, externalErrorMessage )

class BadGatewayError(description: String, externalErrorMessage: String? = null) : ImeiLookupError(description, externalErrorMessage)