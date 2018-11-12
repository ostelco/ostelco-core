package org.ostelco.jsonValidation

import org.everit.json.schema.Schema
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import org.ostelco.JsonSchema
import java.io.*
import java.nio.charset.Charset
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
            val msg = e.message
            throw WebApplicationException("Syntax error in schema description  named '$name'. Error:  ${msg}", Response.Status.INTERNAL_SERVER_ERROR)
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

    @Throws(IOException::class)
    private fun toByteArray(input: InputStream): ByteArray {
        ByteArrayOutputStream().use { output ->
            copy(input, output)
            return output.toByteArray()
        }
    }

    @Throws(IOException::class)
    private fun copy(input: InputStream, output: OutputStream): Int {
        val count = copyLarge(input, output,ByteArray(512))
        return if (count > Integer.MAX_VALUE) {
            -1
        } else count.toInt()
    }

    @Throws(IOException::class)
    fun copyLarge(input: InputStream, output: OutputStream, buffer: ByteArray): Long {
        var count: Long = 0
        var n: Int = input.read(buffer)
        while (-1 != n) {
            output.write(buffer, 0, n)
            count += n.toLong()
            n = input.read(buffer)
        }
        return count
    }


    @Throws(IOException::class, WebApplicationException::class)
    override fun aroundReadFrom(ctx: ReaderInterceptorContext): Any {
        val originalStream = ctx.inputStream
        val originalByteArray = toByteArray(originalStream)
        val body: String = String(originalByteArray, Charset.forName("UTF-8"))

        validator.validateString(ctx.type, body, Response.Status.BAD_REQUEST)

        ctx.inputStream = ByteArrayInputStream(originalByteArray)
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

        // Now we  write the original entity back
        // to the "filtered" output stream to be transmitted
        // over the wire.
        origin.write(contentBytes)
        ctx.outputStream = origin
    }
}
