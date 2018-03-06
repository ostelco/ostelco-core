package com.telenordigital.prime.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public final class PrimeConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("eventProcessor")
    private final EventProcessorConfiguration eventProcessor = new EventProcessorConfiguration();

    public EventProcessorConfiguration getEventProcessorConfig() {
        return eventProcessor;
    }
}
