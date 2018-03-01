package com.telenordigital.prime.config;

import io.dropwizard.Configuration;

public final class PrimeConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("eventProcessor")
    private final EventProcessorConfiguration eventProcessor = new EventProcessorConfiguration();

    public EventProcessorConfiguration getEventProcessorConfig() {
        return eventProcessor;
    }
}
