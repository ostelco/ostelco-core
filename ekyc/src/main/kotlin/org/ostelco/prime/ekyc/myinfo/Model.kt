package org.ostelco.prime.ekyc.myinfo

import com.fasterxml.jackson.annotation.JsonProperty

data class TokenApiResponse(
        @JvmField
        @JsonProperty("access_token")
        val accessToken: String,

        val scope: String,

        @JvmField
        @JsonProperty("token_type")
        val tokenType: String,

        @JvmField
        @JsonProperty("expires_in")
        val expiresIn: Long)

enum class HttpMethod {
    GET,
    POST
}