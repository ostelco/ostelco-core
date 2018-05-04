package org.ostelco.importer

import io.dropwizard.Application
import io.dropwizard.setup.Environment
import org.slf4j.LoggerFactory


/**
 * Entry point for running the server
 */
fun main(args: Array<String>) {
    ImporterApplication().run(*args)
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