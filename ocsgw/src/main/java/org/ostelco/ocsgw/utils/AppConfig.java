package org.ostelco.ocsgw.utils;

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

    public String getDataStoreType () {
        // OCS_DATASOURCE_TYPE env has higher preference over config.properties
        final String dataSource = System.getenv("OCS_DATASOURCE_TYPE");
        if (dataSource == null || dataSource.isEmpty()) {

            return prop.getProperty("DataStoreType", "Local");
        }
        return dataSource;
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

    public String getPubSubTopicId() {
        return getEnvProperty("PUBSUB_TOPIC_ID");
    }

    public String getPubSubSubscriptionIdForCcr() {
        return getEnvProperty("PUBSUB_CCR_SUBSCRIPTION_ID");
    }

    public String getPubSubSubscriptionIdForActivate() {
        return getEnvProperty("PUBSUB_ACTIVATE_SUBSCRIPTION_ID");
    }


    private String getEnvProperty(String propertyName) {
        // METRICS_GRPC_SERVER env has higher preference over config.properties
        final String value = System.getenv(propertyName);
        if (value == null || value.isEmpty()) {
            throw new Error("No "+ propertyName + " set in env");
        }
        return value;
    }
}
