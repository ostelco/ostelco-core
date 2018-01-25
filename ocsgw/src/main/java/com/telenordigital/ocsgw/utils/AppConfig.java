package com.telenordigital.ocsgw.utils;

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
        return prop.getProperty("DataStoreType", "Local");
    }

    public String getGrpcServer() {
        return prop.getProperty("GrpcServer", "127.0.0.1:8082");
    }

    public boolean encryptGrpc() {
        String encrypt = prop.getProperty("GrpcEnryption", "true");
        return Boolean.getBoolean(encrypt);
    }
}
