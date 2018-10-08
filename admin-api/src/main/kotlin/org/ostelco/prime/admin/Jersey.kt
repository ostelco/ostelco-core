package org.ostelco.prime.admin

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.ostelco.prime.getLogger
import java.io.InputStream
import java.lang.reflect.Type
import javax.ws.rs.Consumes
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.ext.MessageBodyReader

/// XXX This is a very generic message body reader, should
//      be available anywhere we read yaml files.
@Consumes("text/vnd.yaml")
class YamlMessageBodyReader : MessageBodyReader<Any> {

    private val logger by getLogger()

    override fun isReadable(
            type: Class<*>,
            genericType: Type,
            annotations: Array<Annotation>,
            mediaType: MediaType): Boolean = true

    override fun readFrom(
            type: Class<Any>,
            genericType: Type,
            annotations: Array<Annotation>, mediaType: MediaType,
            httpHeaders: MultivaluedMap<String, String>,
            inputStream: InputStream): Any {

        try {
            val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
            return mapper.readValue(inputStream, type)
        } catch (e: Exception) {
            logger.error("Failed to parse yaml: ${e.message}")
            throw WebApplicationException(e.message, BAD_REQUEST.statusCode)
        }
    }
}

/**
 * Common 'helper' functions for resources.
 *
 */
val objectMapper = jacksonObjectMapper()

fun <R : Any> R.asJson(`object`: Any): String {
    try {
        return objectMapper.writeValueAsString(`object`)
    } catch (e: JsonProcessingException) {
        val logger by getLogger()
        logger.error("Error in json response {}", e)
    }
    return ""
}