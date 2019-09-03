package org.ostelco.prime.slack

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.AppenderBase
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.logging.AbstractAppenderFactory
import io.dropwizard.logging.async.AsyncAppenderFactory
import io.dropwizard.logging.filter.LevelFilterFactory
import io.dropwizard.logging.layout.LayoutFactory
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.slf4j.event.Level


@JsonTypeName("slack")
class SlackAppenderFactory : AbstractAppenderFactory<ILoggingEvent>() {

    override fun build(
            context: LoggerContext?,
            applicationName: String?,
            layoutFactory: LayoutFactory<ILoggingEvent>?,
            levelFilterFactory: LevelFilterFactory<ILoggingEvent>?,
            asyncAppenderFactory: AsyncAppenderFactory<ILoggingEvent>?): Appender<ILoggingEvent> {

        val appender = SlackAppender()
        appender.name = "slack-appender"
        appender.context = context
        appender.addFilter(levelFilterFactory?.build(threshold))
        filterFactories.forEach { f -> appender.addFilter(f.build()) }
        appender.start()
        return wrapAsync(appender, asyncAppenderFactory)
    }
}

class SlackAppender : AppenderBase<ILoggingEvent>() {

    override fun append(eventObject: ILoggingEvent?) {
        if (eventObject != null) {
            if (eventObject.marker == NOTIFY_OPS_MARKER) {
                SlackNotificationReporter.notifyEvent(
                        level = Level.valueOf(eventObject.level.levelStr),
                        message = eventObject.formattedMessage)
            }
        }
    }
}