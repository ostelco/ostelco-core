package org.ostelco.prime.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.filter.LevelFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.logging.filter.FilterFactory

@JsonTypeName("level-filter-factory")
class LogbackLevelFilterFactory : FilterFactory<ILoggingEvent> {

    var level: Level? = null
    var onMatch: FilterReply? = null
    var onMismatch: FilterReply? = null

    override fun build(): Filter<ILoggingEvent> {
        val levelFilter = LevelFilter()
        level?.also { level ->
            levelFilter.setLevel(level)
            onMatch?.also { onMatch ->
                levelFilter.onMatch = onMatch
            }
            onMismatch?.also { onMismatch ->
                levelFilter.onMismatch = onMismatch
            }
            levelFilter.start()
        }
        return levelFilter
    }
}