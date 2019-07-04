package org.ostelco.prime.auth.apple

enum class GrantType {
    authorization_code,
    refresh_token
}

data class TokenResponse(
        val access_token: String,
        val expires_in: Long,
        val id_token: String,
        val refresh_token: String,
        val token_type: String
)

data class ErrorResponse(val error: Error)

enum class Error {
    invalid_request,
    invalid_client,
    invalid_grant,
    unauthorized_client,
    unsupported_grant_type,
    invalid_scope,
}

data class JWKKey(
        val alg: String,
        val e: String,
        val kid: String,
        val kty: String,
        val n: String,
        val use: String
)

data class JWKSet(val keys: Collection<JWKKey>)