package org.ostelco.jsonValidation

import org.everit.json.schema.Schema
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import org.ostelco.JsonSchema
import java.io.*
import java.nio.charset.Charset
import java.util.stream.Collectors
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.*


class JsonSchemaValidator() {
    private val schemaRoot = "/es2schemas"
    private var schemaMap: MutableMap<String, Schema> = mutableMapOf()


    private fun loadJsonSchemaResource(name: String): Schema {
        val inputStream = this.javaClass.getResourceAsStream("${schemaRoot}/${name}.json")
        if (inputStream == null) {
            throw WebApplicationException("Unknown schema map", Response.Status.INTERNAL_SERVER_ERROR)
        }
        try {
            val jsonEncodedSchemaDescription = JSONObject(JSONTokener(inputStream))
            return org.everit.json.schema.loader.SchemaLoader.load(jsonEncodedSchemaDescription)
        } catch (e: JSONException) {
            throw WebApplicationException("Syntax error in schema description", Response.Status.INTERNAL_SERVER_ERROR)
        }
    }


    private fun getSchema(name: String): Schema {
        if (!schemaMap.containsKey(name)) {
            schemaMap[name] = loadJsonSchemaResource(name)
        }
        return schemaMap[name]!!
    }

    public fun validateString(payloadClass: Class<*>, body: String, error: Response.Status) {
        val schemaAnnotation = payloadClass.getAnnotation<JsonSchema>(JsonSchema::class.java)
        if (schemaAnnotation != null) {
            try {
                getSchema(schemaAnnotation.schemaKey).validate(JSONObject(body))
            } catch (t: Exception) {
                throw WebApplicationException(t.message, error)
            }
        }
    }
}


@Provider
class RequestServerReaderWriterInterceptor : ReaderInterceptor, WriterInterceptor {

    val validator = JsonSchemaValidator()

    @Throws(IOException::class, WebApplicationException::class)
    override fun aroundReadFrom(ctx: ReaderInterceptorContext): Any {
        val originalStream = ctx.inputStream
        val stream = BufferedReader(InputStreamReader(originalStream)).lines()
        val body: String = stream.collect(Collectors.joining("\n"))

        validator.validateString(ctx.type, body, Response.Status.BAD_REQUEST)

        ctx.inputStream = ByteArrayInputStream("$body".toByteArray())
        return ctx.proceed()
    }

    @Throws(IOException::class, WebApplicationException::class)
    override fun aroundWriteTo(ctx: WriterInterceptorContext) {

        // Switch out the original output stream with a
        // ByteArrayOutputStream that we can get a byte
        // array out of
        val origin = ctx.outputStream!!
        val interceptingStream =  ByteArrayOutputStream()
        ctx.outputStream = interceptingStream

        // Proceed, meaning that we'll get all the input
        // sent into the byte output stream.
        ctx.proceed()

        // Then get the byte array & convert it to a nice
        // UTF-8 string
        val contentBytes = interceptingStream.toByteArray()
        val contentString: String = String(contentBytes, Charset.forName("UTF-8"))

        // Validate our now serialized input.
        val type = ctx.entity::class.java
        validator.validateString(ctx.type, contentString, Response.Status.INTERNAL_SERVER_ERROR)

        // Now we convert write the original entity back
        // to the filter output stream to be transmitted
        // over the wire.
        origin.write(contentBytes)
        ctx.outputStream = origin
    }
}
