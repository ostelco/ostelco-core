package org.ostelco.bqmetrics;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.ostelco.bqmetrics.backend.DatabaseBackend;

/**
 * Main entry point to the bq-metrics-extractor API server.
 */
public final class BqMetricsExtractorApplication extends Application<BqMetricsExtractorConfiguration> {

    public static void main(final String[] args) throws Exception {
        new BqMetricsExtractorApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<BqMetricsExtractorConfiguration> bootstrap) {

    }

    @Override
    public void run(final BqMetricsExtractorConfiguration configuration, final Environment environment) {
        //DatabaseBackend databaseBackend = configuration.getDatabaseBackend(environment);
    }
}
