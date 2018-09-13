package org.ostelco.prime.core

open class InternalError(open val errorType: ErrorType )


enum class ErrorType(val errorType: Int) {
    INFORMATIONAL(100),
    CLIENT_ERROR(400),
    SERVER_ERROR(500)
}