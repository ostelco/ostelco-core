package org.ostelco.prime.model

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

// Helper for deserializing  boolean from a string value (TRUE or FALSE)
class StringBooleanDeserializer : JsonDeserializer<Boolean>() {

    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Boolean? {
        val TRUE = "TRUE"
        val FALSE = "FALSE"
        val currentToken = jp.getCurrentToken()

        if (currentToken.equals(JsonToken.VALUE_STRING)) {
            val text = jp.getText().trim()

            if (TRUE.equals(text, ignoreCase = true)) {
                return true
            } else if (FALSE.equals(text, ignoreCase = true)) {
                return false
            }
        } else if (currentToken.equals(JsonToken.VALUE_TRUE)) {
            return true
        }

        return false
    }
}
