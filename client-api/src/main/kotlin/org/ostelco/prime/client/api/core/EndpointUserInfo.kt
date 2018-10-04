package org.ostelco.prime.client.api.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.ostelco.prime.getLogger
import java.util.*


/**
 * If running behind a Google Cloud Endpoint service the Endpoint service will add some
 * information about the user in a HTTP header named:
 *
 * X-Endpoint-API-UserInfo
 *
 * This class can be used to 'capture' this information if present.
 *
 * To capture the information, add the following to the resource class:
 *
 * Valid @HeaderParam("X-Endpoint-API-UserInfo") EndpointUserInfo userInfo
 *
 * Ref.: https://cloud.google.com/endpoints/docs/openapi/authenticating-users
 * Section: Receiving auth results in your API
 */
class EndpointUserInfo(enc: String) {

    private val logger by getLogger()

    private val mapper = jacksonObjectMapper()

    private val obj: JsonNode = mapper.readTree(decode(enc))
    private fun decode(enc: String): String = String(Base64.getDecoder().decode(enc))

    val issuer: String?
        get() = get("issuer")

    val id: String?
        get() = get("id")

    val email: String?
        get() = get("email")

    private operator fun get(key: String): String? = obj.get(key)?.textValue()

    override fun toString(): String = obj.toString()
}
