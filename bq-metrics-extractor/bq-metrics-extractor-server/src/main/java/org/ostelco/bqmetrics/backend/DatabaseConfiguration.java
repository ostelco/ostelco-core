package org.ostelco.bqmetrics.backend;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;
import org.skife.jdbi.v2.DBI;

public class DatabaseConfiguration {
    protected final DataSourceFactory dataSourceFactory;

    public DatabaseConfiguration (
            @JsonProperty("connection") DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    public DataSourceFactory getDataSourceFactory() {
        return dataSourceFactory;
    }

    public DBI createDBI(Environment environment, String name) {
        DBIFactory factory = new DBIFactory();
        return factory.build(environment, dataSourceFactory, name);
    }
}
