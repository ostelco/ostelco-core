package org.ostelco.prime.admin.api

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.ostelco.prime.admin.importer.ImportDeclaration
import org.ostelco.prime.admin.importer.ImportProcessor
import org.ostelco.prime.logger
import java.io.InputStream
import java.lang.reflect.Type
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.ext.MessageBodyReader


/**
 * Resource used to handle the importer related REST calls.
 */
@Path("/importer")
class ImporterResource(val processor: ImportProcessor) {

    private val logger by logger()

    @POST
    @Consumes("text/vnd.yaml")
    fun postStatus(declaration: ImportDeclaration): Response {
        logger.info("POST status for importer")

        return processor.import(declaration).fold(
                    { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                    { Response.status(Response.Status.CREATED) }
        ).build()
    }
}

/// XXX This is a very generic message body reader, should
//      be available anywhere we read yaml files.
@Consumes("text/vnd.yaml")
class YamlMessageBodyReader : MessageBodyReader<Any> {

    private val logger by logger()

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
val objectMapper = ObjectMapper()

fun <R : Any> R.asJson(`object`: Any): String {
    try {
        return objectMapper.writeValueAsString(`object`)
    } catch (e: JsonProcessingException) {
        val logger by logger()
        logger.error("Error in json response {}", e)
    }
    return ""
}