package org.ostelco.prime.admin.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.ostelco.prime.admin.importer.ImportDeclaration
import org.ostelco.prime.admin.importer.ImportProcessor
import org.ostelco.prime.logger
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Response
import jdk.nashorn.internal.runtime.ScriptingFunctions.readLine
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
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

        return try {
            val result: Boolean = processor.import(declaration)

            if (result) {
                Response.ok().build()
            } else {
                Response.status(Response.Status.BAD_REQUEST).build()
            }
        } catch (e: Exception) {
            logger.error("Failed to Import", e)
            Response.serverError().build()
        }
    }
}

/// XXX This is a very generic message body reader, should
//      be available anywhere we read yaml files.
@Consumes("text/vnd.yaml")
class YamlMessageBodyReader : MessageBodyReader<Any> {

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

        val mapper = ObjectMapper(YAMLFactory())
        return mapper.readValue(inputStream, type)
    }
}
