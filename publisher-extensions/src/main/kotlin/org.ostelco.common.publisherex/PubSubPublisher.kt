package org.ostelco.common.publisherex

import java.time.Instant
import io.dropwizard.lifecycle.Managed
import com.google.pubsub.v1.PubsubMessage

/**
 * Abstraction for a point-in-time analytics event.
 *
 * @property timestamp time at which the event occurs
 */
open class Event(val timestamp: Instant = Instant.now()) {
    fun toJsonByteString() = CommonPubSubJsonSerializer.toJsonByteString(this)
}

interface PubSubPublisher : Managed {
    fun publishPubSubMessage(pubsubMessage: PubsubMessage)
    fun publishEvent(event: Event)
}
