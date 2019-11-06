package org.ostelco.prime.analytics.publishers

import com.google.pubsub.v1.PubsubMessage
import io.dropwizard.lifecycle.Managed
import org.ostelco.prime.analytics.events.Event

interface PubSubPublisher : Managed {
    fun publishPubSubMessage(pubsubMessage: PubsubMessage)
    fun publishEvent(event: Event)
}
