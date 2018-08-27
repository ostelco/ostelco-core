package org.ostelco.ocsgw.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);

    private final Properties prop = new Properties();

    public AppConfig() throws IOException {
        final String fileName = "config.properties";
        InputStream iStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        prop.load(iStream);
        iStream.close();
    }

    public String getDataStoreType () {
        return prop.getProperty("DataStoreType", "Local");
    }

    public String getGrpcServer() {
        // GRPC_SERVER env has higher preference over config.properties
        final String grpcServer = System.getenv("GRPC_SERVER");
        if (grpcServer == null || grpcServer.isEmpty()) {
            return prop.getProperty("GrpcServer", "127.0.0.1:8082");
        }
        return grpcServer;
    }

    public String getMetricsServer() {
        // GRPC_SERVER env has higher preference over config.properties
        final String metricsServer = System.getenv("METRICS_SERVER");
        if (metricsServer == null || metricsServer.isEmpty()) {
            LOG.warn("No metric server set in env");
        }
        return metricsServer;
    }
}
