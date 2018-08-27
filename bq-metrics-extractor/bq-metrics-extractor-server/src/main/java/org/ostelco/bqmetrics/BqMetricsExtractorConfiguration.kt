package org.ostelco.bqmetrics

import com.google.common.base.Preconditions.checkNotNull

import org.ostelco.bqmetrics.backend.DatabaseBackend
import org.ostelco.bqmetrics.backend.DatabaseConfiguration
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment

/**
 * Server configuration for the bq-metrics-extractor API server.
 */
class BqMetricsExtractorConfiguration @JsonCreator
constructor(
        @param:JsonProperty("database") val databaseConfiguration: DatabaseConfiguration) : Configuration() {

    init {
        checkNotNull(databaseConfiguration)
    }

    fun getDatabaseBackend(environment: Environment): DatabaseBackend {
        return databaseConfiguration.createDBI(environment, "jdbi-backend").onDemand(DatabaseBackend::class.java)
    }
}
