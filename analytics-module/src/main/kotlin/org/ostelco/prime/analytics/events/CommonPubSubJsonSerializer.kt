package org.ostelco.prime.analytics.events

import com.google.gson.Gson
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.protobuf.ByteString
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

object CommonPubSubJsonSerializer {
    private val gson = Gson()
            .newBuilder()
            .registerTypeAdapter(Instant::class.java, JsonSerializer<Instant> { src, _, _ ->
                JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(src))
            })
            .registerTypeAdapter(Currency::class.java, JsonSerializer<Currency> { src, _, _ ->
                JsonPrimitive(src.currencyCode)
            })
            .create()

    fun toJson(event: Event): String = gson.toJson(event)
    fun toJsonByteString(event: Event): ByteString = ByteString.copyFromUtf8(toJson(event))
}
