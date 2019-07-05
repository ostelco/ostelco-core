package org.ostelco.prime.jwt

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.objectMapper
import java.io.IOException
import java.util.*

object JwtParser {

    private val logger by getLogger()

    fun getClaims(jwt: String): JsonNode? {

        if (jwt.count { it == '.' } != 2) {
            logger.warn("The provided token is an Invalid JWT token")
            return null
        }

        val parts = jwt.split('.').dropLastWhile { it.isEmpty() }.toTypedArray()

        return String(Base64.getDecoder().decode(parts[1]
                .replace("-", "+")
                .replace("_", "/")))
                .let(::decodeClaims)
    }

    /* Decodes the claims part of a JWT token.
       Returns null on error. */
    private fun decodeClaims(claims: String): JsonNode? {
        try {
            return objectMapper.readTree(claims)
        } catch (e: JsonParseException) {
            logger.error("Parsing of the provided json doc {} failed: {}", claims, e)
        } catch (e: IOException) {
            logger.error("Unexpected error when parsing the json doc {}: {}", claims, e)
        }
        return null
    }
}