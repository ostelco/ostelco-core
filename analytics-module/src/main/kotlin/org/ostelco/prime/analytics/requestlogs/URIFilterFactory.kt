package org.ostelco.prime.analytics.requestlogs

import ch.qos.logback.access.spi.IAccessEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.logging.filter.FilterFactory
import org.hibernate.validator.constraints.NotBlank

@JsonTypeName("URI")
class URIFilterFactory : FilterFactory<IAccessEvent> {

    @NotBlank
    @JsonProperty
    lateinit var uri: String

    override fun build() = object : Filter<IAccessEvent>() {
        override fun decide(event: IAccessEvent): FilterReply {
            return if (event.requestURI == "/$uri") {
                FilterReply.DENY
            } else {
                FilterReply.NEUTRAL
            }
        }
    }
}