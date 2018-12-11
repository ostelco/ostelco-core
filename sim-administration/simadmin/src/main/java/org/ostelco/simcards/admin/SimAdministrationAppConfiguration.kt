package org.ostelco.simcards.admin

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration
import io.dropwizard.db.DataSourceFactory
import javax.validation.Valid
import javax.validation.constraints.NotNull


class SimAdministrationAppConfiguration : Configuration() {
    @Valid
    @NotNull
    @JsonProperty("database")
    var database: DataSourceFactory = DataSourceFactory()


    @Valid
    @NotNull
    @JsonProperty
    private val httpClient = JerseyClientConfiguration()

    fun getJerseyClientConfiguration(): JerseyClientConfiguration {
        return httpClient
    }

}