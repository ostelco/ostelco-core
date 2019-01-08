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
        // OCS_GRPC_SERVER env has higher preference over config.properties
        final String grpcServer = System.getenv("OCS_GRPC_SERVER");
        if (grpcServer == null || grpcServer.isEmpty()) {
            throw new Error("No OCS_GRPC_SERVER set in env");
        }
        return grpcServer;
    }

    public String getMetricsServer() {
        // METRICS_GRPC_SERVER env has higher preference over config.properties
        final String metricsServer = System.getenv("METRICS_GRPC_SERVER");
        if (metricsServer == null || metricsServer.isEmpty()) {
            throw new Error("No METRICS_GRPC_SERVER set in env");
        }
        return metricsServer;
    }
}
