package org.ostelco.logging


import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.cloud.logging.logback.LoggingAppender
import io.dropwizard.logging.AbstractAppenderFactory
import io.dropwizard.logging.async.AsyncAppenderFactory
import io.dropwizard.logging.filter.LevelFilterFactory
import io.dropwizard.logging.layout.LayoutFactory

@JsonTypeName("gcp")
class GcpAppenderFactory : AbstractAppenderFactory<ILoggingEvent>() {

    override fun build(
            context: LoggerContext?,
            applicationName: String?,
            layoutFactory: LayoutFactory<ILoggingEvent>?,
            levelFilterFactory: LevelFilterFactory<ILoggingEvent>?,
            asyncAppenderFactory: AsyncAppenderFactory<ILoggingEvent>?): Appender<ILoggingEvent> {

        val appender = LoggingAppender()
        appender.name = "gcp-appender"
        appender.context = context

        if (levelFilterFactory != null) {
            appender.addFilter(levelFilterFactory.build(threshold))
        }
        filterFactories.forEach { f -> appender.addFilter(f.build()) }
        appender.start()
        return wrapAsync(appender, asyncAppenderFactory)
    }
}
