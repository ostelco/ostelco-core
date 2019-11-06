package org.ostelco.prime.pubsub

import com.google.api.core.ApiFuture
import com.google.pubsub.v1.PubsubMessage
import io.dropwizard.lifecycle.Managed
import java.util.concurrent.ScheduledExecutorService


interface PubSubPublisher2 : Managed {
    fun publishPubSubMessage(pubsubMessage: PubsubMessage): ApiFuture<String>
}