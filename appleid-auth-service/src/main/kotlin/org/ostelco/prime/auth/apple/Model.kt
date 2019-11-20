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

enum class Error(val cause: String) {
    invalid_request("The request is malformed, normally due to a missing parameter, contains an unsupported parameter, includes multiple credentials, or uses more than one mechanism for authenticating the client."),
    invalid_client("The client authentication failed."),
    invalid_grant("The authorization grant or refresh token is invalid."),
    unauthorized_client("The client is not authorized to use this authorization grant type."),
    unsupported_grant_type("The authenticated client is not authorized to use the grant type."),
    invalid_scope("The requested scope is invalid."),
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
