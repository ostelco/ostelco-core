package org.ostelco.diameter

data class SessionContext(
        val sessionId: String,
        val originHost: String,
        val originRealm: String
        )
