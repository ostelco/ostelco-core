package org.ostelco.jsonschema

import org.everit.json.schema.Schema
import org.everit.json.schema.SchemaException
import org.everit.json.schema.ValidationException
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import javax.ws.rs.WebApplicationException
import javax.ws.rs.container.DynamicFeature
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.FeatureContext
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider
import javax.ws.rs.ext.ReaderInterceptor
import javax.ws.rs.ext.ReaderInterceptorContext
import javax.ws.rs.ext.WriterInterceptor
import javax.ws.rs.ext.WriterInterceptorContext


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class JsonSchema(val schemaKey: String)


/**
 * This is a function to which the member variable of type [org.slf4j.Logger] is delegated to be instantiated.
 * The syntax to do so is <code>private val logger by getLogger()</code>.
 * This function will then return the [org.slf4j.Logger] for calling class.
 */
fun <R : Any> R.getLogger(): Lazy<Logger> = lazy {
    LoggerFactory.getLogger(this.javaClass)
}


class JsonSchemaValidator {
    private val schemaRoot = "/es2schemas"
    private var schemaMap: MutableMap<String, Schema> = mutableMapOf()

    private val logger by getLogger()

    private fun loadJsonSchemaResource(name: String): Schema {
        val inputStream = this.javaClass.getResourceAsStream("$schemaRoot/$name.json") ?: throw WebApplicationException("Unknown schema map: '$name'", Response.Status.INTERNAL_SERVER_ERROR)
        try {
            val jsonEncodedSchemaDescription = JSONObject(JSONTokener(inputStream))
            return org.everit.json.schema.loader.SchemaLoader.load(jsonEncodedSchemaDescription)
        } catch (e: JSONException) {
            val msg = e.message
            logger.error("Syntax error in schema description  named '$name'. Error:  $msg", e)
            throw WebApplicationException("Syntax error in schema description  named '$name'. Error:  $msg", Response.Status.INTERNAL_SERVER_ERROR)
        } catch (e: SchemaException) {
            logger.error("Illegal Schema definition for schema: '$name'.  Error: ${e.message}", e)
            throw WebApplicationException("Illegal Schema definition for schema: '$name'.  Error: ${e.message}")
        }
    }

    private fun getSchema(name: String): Schema = schemaMap.getOrPut(name) { loadJsonSchemaResource(name) }

    @Throws(WebApplicationException::class)
    fun validateString(payloadClass: Class<*>, body: String, error: Response.Status) {
        val schemaAnnotation = payloadClass.getAnnotation<JsonSchema>(JsonSchema::class.java)
        if (schemaAnnotation != null) {
            try {
                getSchema(schemaAnnotation.schemaKey).validate(JSONObject(body))
            } catch (t: ValidationException) {
                var causes = t.causingExceptions.joinToString(separator = ". ") { e: ValidationException -> "${e.keyword}: ${e.errorMessage}" }
                if (causes.isBlank()) {
                    causes = t.errorMessage
                }
                val msg = "Schema validation failed while validating schema named: '${schemaAnnotation.schemaKey}'.  Error:  $t.message. Causes= $causes"
                logger.error(msg, t)
                // XXX The web application exception seems to be swallowed.
                throw WebApplicationException(msg, error)
            }
        }
    }
}

@Provider
class DynamicES2ValidatorAdder : DynamicFeature {

    override fun configure(resourceInfo: ResourceInfo, context: FeatureContext) {
        val allAnnotations = mutableSetOf<Any>()

        resourceInfo.resourceMethod.returnType.annotations.map { allAnnotations.add(it.annotationClass.java) }
        resourceInfo.resourceMethod.parameterTypes.map { it -> it.annotations.map { allAnnotations.add(it.annotationClass.java) }}

        if (allAnnotations.contains(JsonSchema::class.java)) {
             context.register(RequestServerReaderWriterInterceptor::class.java)
        }
    }
}

@Provider
private class RequestServerReaderWriterInterceptor : ReaderInterceptor, WriterInterceptor {

    private val validator = JsonSchemaValidator()

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
        val body = String(originalByteArray, StandardCharsets.UTF_8)

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
        // sent into the byte output (intercepting) stream.
        ctx.proceed()

        // Then get the byte array & convert it to a nice
        // UTF-8 string
        val contentBytes = interceptingStream.toByteArray()
        val contentString  = String(contentBytes, StandardCharsets.UTF_8)

        // Validate our now serialized input.
        validator.validateString(ctx.type, contentString, Response.Status.INTERNAL_SERVER_ERROR)

        // Now we  write the original entity back
        // to the "filtered" output stream to be transmitted
        // over the wire.
        origin.write(contentBytes)
        ctx.outputStream = origin
    }
}
