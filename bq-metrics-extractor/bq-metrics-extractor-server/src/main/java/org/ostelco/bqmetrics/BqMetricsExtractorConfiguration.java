package org.ostelco.bqmetrics;

import static com.google.common.base.Preconditions.checkNotNull;

import org.ostelco.bqmetrics.backend.DatabaseBackend;
import org.ostelco.bqmetrics.backend.DatabaseConfiguration;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

/**
 * Server configuration for the bq-metrics-extractor API server.
 */
public final class BqMetricsExtractorConfiguration extends Configuration {
    private final DatabaseConfiguration databaseConfiguration;

    @JsonCreator
    public BqMetricsExtractorConfiguration(
            @JsonProperty("database") final DatabaseConfiguration database) {
        checkNotNull(database);

        this.databaseConfiguration = database;
    }

    public DatabaseConfiguration getDatabaseConfiguration() { return databaseConfiguration; }

    public DatabaseBackend getDatabaseBackend(Environment environment) {
        return databaseConfiguration.createDBI(environment, "jdbi-backend").onDemand(DatabaseBackend.class);
    }
}
