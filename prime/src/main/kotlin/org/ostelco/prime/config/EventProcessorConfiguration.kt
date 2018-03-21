package org.ostelco.prime.config

import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.validator.constraints.NotEmpty

class EventProcessorConfiguration {

    @NotEmpty
    @JsonProperty("configFile")
    lateinit var configFile: String

    @NotEmpty
    @JsonProperty("projectId")
    lateinit var projectId: String

    @NotEmpty
    @JsonProperty("topicId")
    lateinit var topicId: String
}
