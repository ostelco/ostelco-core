package org.ostelco

import io.dropwizard.Configuration
import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.*
import io.dropwizard.db.DataSourceFactory
import javax.validation.Valid
import io.dropwizard.client.JerseyClientConfiguration


class SimAdministrationAppConfiguration : Configuration() {
    @Valid
    @NotNull
    @JsonProperty("database")
    var database: DataSourceFactory


    @Valid
    @NotNull
    @JsonProperty
    private val httpClient = JerseyClientConfiguration()

    fun getJerseyClientConfiguration(): JerseyClientConfiguration {
        return httpClient
    }

    init {
        database = DataSourceFactory()
    }
}