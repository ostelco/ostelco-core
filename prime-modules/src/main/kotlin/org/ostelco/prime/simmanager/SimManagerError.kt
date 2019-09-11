package org.ostelco.prime.simmanager

import org.ostelco.prime.apierror.InternalError

sealed class SimManagerError(var description: String, val error: InternalError?,  val pingOk: Boolean = false) : InternalError()

class NotFoundError(description: String, error: InternalError? = null, pingOk: Boolean = false) : SimManagerError(description, error = error, pingOk = pingOk)

class NotUpdatedError(description: String, error: InternalError? = null, pingOk: Boolean = false) : SimManagerError(description, error = error, pingOk=pingOk)

class ForbiddenError(description: String, error: InternalError? = null) : SimManagerError(description, error = error)

class AdapterError(description: String, error: InternalError? = null) : SimManagerError(description, error = error)

class DatabaseError(description: String, error: InternalError? = null) : SimManagerError(description, error = error)

class SystemError(description: String, error: InternalError? = null) : SimManagerError(description, error = error)
