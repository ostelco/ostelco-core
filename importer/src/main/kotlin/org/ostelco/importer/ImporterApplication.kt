package org.ostelco.importer

import io.dropwizard.Application
import io.dropwizard.Configuration
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Consumes
import javax.ws.rs.Path
import javax.ws.rs.core.Response
import io.dropwizard.jetty.HttpConnectorFactory
import io.dropwizard.server.DefaultServerFactory
import io.dropwizard.setup.Environment
import org.glassfish.jersey.client.ClientProperties
import org.slf4j.LoggerFactory
import javax.ws.rs.client.Client
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.math.BigDecimal


/**
 * The configuration for Importer.
 */
class ImporterConfig : Configuration() {
}


/**
 * The input classes being parsed (as yaml).
 */

class ProducingAgent(var name: String? = null, var version: String? = null)

class ImportDeclaration(
        var producingAgent: ProducingAgent? = null,
        var offer: Offer? = null
)

class TimeInterval(var from: String?= null, var to: String? = null)

class Presentation(
        var badgeLabel: String? = null,
        var description: String? = null,
        var shortDescription: String? = null,
        var label: String? = null,
        var name: String? = null,
        var priceLabel: String? = null,
        var hidden: Boolean? = null,
        var imageUrl: String? = null
)

class OfferFinancials(
        var repurchability: String? = null,
        var currencyLabel: String? = null,
        var price: Int? = null,
        var taxRate: BigDecimal? = null
)

class Offer(
    var visibility: TimeInterval? = null,
    var presentation: Presentation? = null,
    var financial: OfferFinancials? = null
)

/**
 * Resource used to handle the importer related REST calls.
 */
@Path("/importer")
class ImporterResource(val processor: ImportProcessor) {

    private val LOG = LoggerFactory.getLogger(ImporterResource::class.java)

    /**
     * Get the status
     */
    @GET
    @Path("/get/status")
    fun getStatus(): Response {
        LOG.info("GET status for importer")
        return Response.ok().build()
    }

    /**
     * Upload a new import specification
     */
    @POST
    @Consumes("text/vnd.yaml")
    @Path("")
    fun getStatus(yaml: String): Response {
        LOG.info("POST status for importer")

        return try {
            val mapper = ObjectMapper(YAMLFactory())
            val declaration: ImportDeclaration =
                    mapper.readValue(yaml, ImportDeclaration::class.java)
            val result: Boolean = processor.import(declaration)

            if (result) {
                Response.ok().build()
            } else {
                Response.serverError().build()// Shouldn't be ok, but completion won't work.
            }
        } catch (e: Exception) {
            System.out.println("Cought exception" + e.toString())
            Response.serverError().build()
        }
    }
}

/**
 * Entry point for running the server
 */
fun main(args: Array<String>) {
    ImporterApplication().run(*args)
}

interface ImportProcessor {
    fun import(decl: ImportDeclaration) : Boolean
}

/**
 * Dropwizard application for running pseudonymiser service that
 * converts Data-Traffic PubSub message to a pseudonymised version.
 */
class ImporterApplication : Application<ImporterConfig>() {

    private val LOG = LoggerFactory.getLogger(ImporterApplication::class.java)

    // Run the dropwizard application (called by the kotlin [main] wrapper).
    override fun run(
            config: ImporterConfig,
            env: Environment) {
        val processor: ImportProcessor = object : ImportProcessor {
            public override fun import(decl: ImportDeclaration) : Boolean {
                return true
            }
        }
        env.jersey().register(ImporterResource(processor))
    }
}