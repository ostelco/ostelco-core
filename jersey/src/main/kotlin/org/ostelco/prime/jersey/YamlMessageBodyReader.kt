package org.ostelco.prime.jersey

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
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

@Consumes("text/vnd.yaml")
class YamlMessageBodyReader : MessageBodyReader<Any> {

    private val logger by getLogger()
    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

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
            return mapper.readValue(inputStream, type)
        } catch (e: Exception) {
            logger.error("Failed to parse yaml: ${e.message}")
            throw WebApplicationException(e.message, BAD_REQUEST.statusCode)
        }
    }
}