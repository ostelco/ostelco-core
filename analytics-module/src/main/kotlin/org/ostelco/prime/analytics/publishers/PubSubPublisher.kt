package org.ostelco.prime.analytics.publishers

import com.google.pubsub.v1.PubsubMessage
import io.dropwizard.lifecycle.Managed
import java.util.concurrent.ScheduledExecutorService

interface PubSubPublisher : Managed {
    var singleThreadScheduledExecutor: ScheduledExecutorService
    fun publishPubSubMessage(pubsubMessage: PubsubMessage)
}
