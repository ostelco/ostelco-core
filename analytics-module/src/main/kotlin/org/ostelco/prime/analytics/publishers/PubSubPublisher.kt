package org.ostelco.prime.analytics.publishers

import com.google.pubsub.v1.PubsubMessage
import io.dropwizard.lifecycle.Managed
import java.util.concurrent.ScheduledExecutorService
import org.ostelco.common.publisherex.Event

interface PubSubPublisher1 : Managed {
    var singleThreadScheduledExecutor: ScheduledExecutorService
    fun publishPubSubMessage(pubsubMessage: PubsubMessage)
    fun publishEvent(event: Event)
}
