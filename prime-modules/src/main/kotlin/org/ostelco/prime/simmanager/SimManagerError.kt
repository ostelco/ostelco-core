package org.ostelco.prime.simmanager

import org.ostelco.prime.apierror.InternalError

sealed class SimManagerError(var description: String, val error: InternalError?) : InternalError()

class NotFoundError(description: String, error: InternalError? = null) : SimManagerError(description, error = error)

class NotUpdatedError(description: String, error: InternalError? = null) : SimManagerError(description, error = error)

class ForbiddenError(description: String, error: InternalError? = null) : SimManagerError(description, error = error)

class AdapterError(description: String, error: InternalError? = null) : SimManagerError(description, error = error)

class DatabaseError(description: String, error: InternalError? = null) : SimManagerError(description, error = error)

class SystemError(description: String, error: InternalError? = null) : SimManagerError(description, error = error)
