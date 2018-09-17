package org.ostelco.prime.client.api.core

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.ostelco.prime.logger
import java.io.IOException
import java.util.*
import javax.validation.constraints.NotNull


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

    private val logger by logger()

    private val mapper = ObjectMapper()

    /* Causes an error if decoding of the base64 encoded json doc fails. */
    @NotNull
    private val obj: JsonNode?

    val issuer: Optional<String>
        get() = get("issuer")

    val id: Optional<String>
        get() = get("id")

    val email: Optional<String>
        get() = get("email")

    init {
        var obj: JsonNode? = null
        try {
            obj = mapper.readTree(decode(enc))
        } catch (e: JsonParseException) {
            logger.error("Parsing of the provided json doc {} failed: {}", enc, e)
        } catch (e: IOException) {
            logger.error("Unexpected error when parsing the json doc {}: {}", enc, e)
        }

        this.obj = obj
    }

    fun hasIssuer(): Boolean {
        return has("issuer")
    }

    fun hasId(): Boolean {
        return has("id")
    }

    fun hasEmail(): Boolean {
        return has("email")
    }

    private fun has(key: String): Boolean {
        return obj != null && obj.has(key)
    }

    private operator fun get(key: String): Optional<String> {
        return if (has(key)) Optional.of(obj!!.get(key).textValue()) else Optional.empty()
    }

    private fun decode(enc: String): String {
        return String(Base64.getDecoder().decode(enc))
    }

    override fun toString(): String {
        return obj!!.toString()
    }
}
