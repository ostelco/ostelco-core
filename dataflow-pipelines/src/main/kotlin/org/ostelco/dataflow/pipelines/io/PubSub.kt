package org.ostelco.dataflow.pipelines.io

import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO
import org.ostelco.analytics.api.DataTrafficInfo

// Read from PubSub
fun readFromPubSub(topic: String) = PubsubIO
        .readProtos(DataTrafficInfo::class.java)
        .fromSubscription("projects/pantel-2decb/subscriptions/$topic")
