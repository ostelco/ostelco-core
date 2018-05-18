package org.ostelco.prime

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import org.ostelco.prime.module.PrimeModule
import javax.validation.Valid
import javax.validation.constraints.NotNull

class PrimeConfiguration : Configuration() {

    @Valid
    @NotNull
    @JsonProperty
    lateinit var modules: List<PrimeModule>
}
