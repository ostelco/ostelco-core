package org.ostelco.prime.auth

import java.security.KeyPairGenerator
import java.util.*

const val AUTH_CODE = "AUTH_CODE"
const val TEAM_ID = "TEAM_ID"
const val KEY_ID = "KEY_ID"
const val CLIENT_ID = "CLIENT_ID"
val PRIVATE_KEY: ByteArray = KeyPairGenerator.getInstance("EC")
        .apply { this.initialize(256) }
        .genKeyPair()
        .private
        .encoded
        .let { Base64.getEncoder().encode(it) }