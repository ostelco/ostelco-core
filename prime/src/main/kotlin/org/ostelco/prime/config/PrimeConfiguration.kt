package org.ostelco.prime.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration

import javax.validation.Valid
import javax.validation.constraints.NotNull

class PrimeConfiguration : Configuration() {

    @Valid
    @NotNull
    @JsonProperty("eventProcessor")
    lateinit var eventProcessorConfig: EventProcessorConfiguration

    @Valid
    @NotNull
    @JsonProperty("ocs")
    lateinit var ocsConfiguration: OcsConfig
}
