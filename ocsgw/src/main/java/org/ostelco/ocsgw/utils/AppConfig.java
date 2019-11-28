package org.ostelco.ocsgw.utils;

import org.ostelco.ocsgw.datasource.DataSourceType;
import org.ostelco.ocsgw.datasource.SecondaryDataSourceType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private final Properties prop = new Properties();

    public AppConfig() throws IOException {
        final String fileName = "config.properties";
        InputStream iStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        prop.load(iStream);
        iStream.close();
    }

    public DataSourceType getDataStoreType () {
        // OCS_DATASOURCE_TYPE env has higher preference over config.properties
        final String dataSource = System.getenv("OCS_DATASOURCE_TYPE");
        if (dataSource == null || dataSource.isEmpty()) {
            try {
                return DataSourceType.valueOf(prop.getProperty("DataStoreType", "Local"));
            } catch (IllegalArgumentException e) {
                return DataSourceType.Local;
            }
        }
        try {
            return DataSourceType.valueOf(dataSource);
        } catch (IllegalArgumentException e) {
            return DataSourceType.Local;
        }
    }

    public Long getDefaultRequestedServiceUnit () {
          final String defaultRequestedServiceUnit = System.getProperty("DEFAULT_REQUESTED_SERVICE_UNIT");
          if (defaultRequestedServiceUnit == null || defaultRequestedServiceUnit.isEmpty()) {
              return 40_000_000L;
          } else {
              return Long.parseLong(defaultRequestedServiceUnit);
          }
    }

    public SecondaryDataSourceType getSecondaryDataStoreType () {
        // OCS_SECONDARY_DATASOURCE_TYPE env has higher preference over config.properties
        final String secondaryDataSource = System.getenv("OCS_SECONDARY_DATASOURCE_TYPE");
        if (secondaryDataSource == null || secondaryDataSource.isEmpty()) {
            try {
                return SecondaryDataSourceType.valueOf(prop.getProperty("SecondaryDataStoreType", "PubSub"));
            } catch (IllegalArgumentException e) {
                return SecondaryDataSourceType.PubSub;
            }
        }
        try {
            return SecondaryDataSourceType.valueOf(secondaryDataSource);
        } catch (IllegalArgumentException e) {
            return SecondaryDataSourceType.PubSub;
        }
    }

    public String getGrpcServer() {
        return getEnvProperty("OCS_GRPC_SERVER");
    }

    public String getPubSubProjectId() {
        return getEnvProperty("PUBSUB_PROJECT_ID");
    }

    public String getPubSubTopicIdForCcr() {
        return getEnvProperty("PUBSUB_CCR_TOPIC_ID");
    }

    public String getPubSubTopicIdForCca() {
        return getEnvProperty("PUBSUB_CCA_TOPIC_ID");
    }

    public String getPubSubSubscriptionIdForCca() {
        return getEnvProperty("PUBSUB_CCA_SUBSCRIPTION_ID");
    }

    public String getPubSubSubscriptionIdForActivate() {
        return getEnvProperty("PUBSUB_ACTIVATE_SUBSCRIPTION_ID");
    }


    private String getEnvProperty(String propertyName) {
        final String value = System.getenv(propertyName);
        if (value == null || value.isEmpty()) {
            throw new Error("No "+ propertyName + " set in env");
        }
        return value;
    }
}
