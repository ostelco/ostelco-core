package org.ostelco.prime.storage.documentstore

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.google.cloud.Timestamp
import org.ostelco.prime.store.datastore.DatastoreExcludeFromIndex

data class CustomerActivity(
        @JsonDeserialize(using = TimestampDeserializer::class) val timestamp: Timestamp,
        val severity: String,
        @DatastoreExcludeFromIndex val message: String
)

class TimestampDeserializer : StdDeserializer<Timestamp>(Timestamp::class.java) {

    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): Timestamp {
        val node: JsonNode = parser.codec.readTree(parser)
        val seconds = node["seconds"].longValue()
        val nanos = node["nanos"].intValue()
        return Timestamp.ofTimeSecondsAndNanos(seconds, nanos)
    }
}