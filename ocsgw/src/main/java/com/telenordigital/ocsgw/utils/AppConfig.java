package com.telenordigital.ocsgw.utils;


import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class AppConfig {

    private static final Logger logger = Logger.getLogger(AppConfig.class);
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
}
