package org.ostelco.ocsgw.utils;

import org.ostelco.ocsgw.datasource.DataSourceType;

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
        return getDataSourceType("OCS_DATASOURCE_TYPE", "DataSourceType", "Local");
    }

    public Long getDefaultRequestedServiceUnit () {
          final String defaultRequestedServiceUnit = System.getProperty("DEFAULT_REQUESTED_SERVICE_UNIT");
          if (defaultRequestedServiceUnit == null || defaultRequestedServiceUnit.isEmpty()) {
              return 40_000_000L;
          } else {
              return Long.parseLong(defaultRequestedServiceUnit);
          }
    }

    public DataSourceType getSecondaryDataSourceType () {
        return getDataSourceType("OCS_SECONDARY_DATASOURCE_TYPE", "SecondaryDataSourceType", "PubSub");
    }

    public DataSourceType getMultiInitDataSourceType () {
        return getDataSourceType("OCS_MULTI_INIT_DATASOURCE_TYPE", "MultiInitDataSourceType", "PubSub");
    }

    public DataSourceType getMultiUpdateDataSourceType () {
        return getDataSourceType("OCS_MULTI_UPDATE_DATASOURCE_TYPE", "MultiUpdateDataSourceType", "PubSub");
    }

    public DataSourceType getMultiTerminateDataSourceType () {
        return getDataSourceType("OCS_MULTI_TERMINATE_DATASOURCE_TYPE", "MultiTerminateDataSourceType", "PubSub");
    }

    public DataSourceType getMultiActivateDataSourceType () {
        return getDataSourceType("OCS_MULTI_ACTIVATE_DATASOURCE_TYPE", "MultiActivateDataSourceType", "PubSub");
    }

    private DataSourceType getDataSourceType(final String envName, final String propertyName, final String defaultSourceType) {
        // env has higher preference over config.properties
        final String dataSource = System.getenv(envName);
        if (dataSource == null || dataSource.isEmpty()) {
            try {
                return DataSourceType.valueOf(prop.getProperty(propertyName, defaultSourceType));
            } catch (IllegalArgumentException e) {
                return DataSourceType.PubSub;
            }
        }
        try {
            return DataSourceType.valueOf(dataSource);
        } catch (IllegalArgumentException e) {
            return DataSourceType.PubSub;
        }
    }

    public String getGrpcServer() {
        return getEnvProperty("OCS_GRPC_SERVER");
    }

    public String getMetricsServer() {
        return getEnvProperty("METRICS_GRPC_SERVER");
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
