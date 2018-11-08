package org.ostelco.jsonValidation

import org.everit.json.schema.Schema
import org.everit.json.schema.ValidationException
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import org.ostelco.JsonSchema
import java.util.stream.Collectors
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.*
import java.io.*
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseFilter






class JsonSchemaValidator() {
    private val schemaRoot = "/es2schemas"
    private var schemaMap: MutableMap<String, Schema> = mutableMapOf()


    private fun loadJsonSchemaResource(name: String) : Schema {
        val inputStream = this.javaClass.getResourceAsStream("${schemaRoot}/${name}.json")
        if (inputStream == null) {
            throw WebApplicationException("Unknown schema map", Response.Status.INTERNAL_SERVER_ERROR)
        }
        try {
            val schema = JSONObject(JSONTokener(inputStream))
            return org.everit.json.schema.loader.SchemaLoader.load(schema)
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

    public fun validateString(payloadClass: Class<*>, body:String) {
        val schemaAnnotation = payloadClass.getAnnotation<JsonSchema>(JsonSchema::class.java)
        if (schemaAnnotation != null) {
            try {
                getSchema(schemaAnnotation.schemaKey).validate(JSONObject(body))
            } catch (t: ValidationException) {
                throw WebApplicationException(t.errorMessage, Response.Status.BAD_REQUEST)
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
        ctx.inputStream = ByteArrayInputStream("$body".toByteArray())

        validator.validateString(ctx.type, body)

        return ctx.proceed()
    }
    

    @Throws(IOException::class, WebApplicationException::class)
    override fun aroundWriteTo(ctx: WriterInterceptorContext) {

        // Read the whole output entity as a byte array,
        // then convert it to a string that is validated
        val out =  ByteArrayOutputStream()
        ctx.outputStream = out
        val contentBytes = out.toByteArray()
        val body = contentBytes.contentToString()
        val type = ctx.entity::class.java
        validator.validateString(ctx.type, body)

        // Now we convert write the original entity back
        // to the filter output stream.
        ctx.outputStream = ByteArrayOutputStream()
        ctx.outputStream.write(out.toByteArray())
        ctx.proceed()
    }
}
