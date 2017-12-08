package com.telenordigital.prime.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public final class EventProcessorConfiguration {
    @NotEmpty
    @JsonProperty("databaseName")
    private String databaseName;

    @NotEmpty
    @JsonProperty("configFile")
    private String configFile;

    public String getDatabaseName() {
        return databaseName;
    }

    public String getConfigFile() {
        return configFile;
    }
}
