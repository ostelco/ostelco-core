package org.ostelco.simcards.inventory

import org.ostelco.prime.apierror.InternalError

sealed class DatabaseError(val msg: String, val details: String? = null, val error: InternalError?) :
        InternalError()

class NotFoundError(msg: String, error: InternalError? = null) :
        DatabaseError(msg, error = error)

class AlreadyExistsError(msg: String, error: InternalError? = null) :
        DatabaseError(msg, error = error)

class NotUpdatedError(msg: String, error: InternalError? = null) :
        DatabaseError(msg, error = error)

class NotDeletedError(msg: String, error: InternalError? = null) :
        DatabaseError(msg, error = error)

class InternalError(msg: String, details: String, error: InternalError? = null) :
        DatabaseError(msg, details, error = error)

class Error(msg: String, details: String, error: InternalError? = null) :
        DatabaseError(msg, details, error = error)