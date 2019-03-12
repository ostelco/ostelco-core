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

enum class PersonApiAttributes(val label: String) {
    NAME("name"),
    SEX("sex"),
    DOB("dob"),
    RESIDENTIAL_STATUS("residentialstatus"),
    NATIONALITY("nationality"),
    MOBILE_NO("mobileno"),
    EMAIL("email"),
    REG_ADD("regadd"),
}

enum class HttpMethod {
    GET,
    POST
}