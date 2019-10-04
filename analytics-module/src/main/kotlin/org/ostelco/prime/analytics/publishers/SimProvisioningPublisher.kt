package org.ostelco.prime.analytics.publishers

import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.analytics.events.SimProvisioningEvent

/**
 * This holds logic for sending SIM provisioning events to Cloud Pub/Sub.
 */
object SimProvisioningPublisher :
        PubSubPublisher by DelegatePubSubPublisher(topicId = ConfigRegistry.config.simProvisioningTopicId) {

    fun publish(subscriptionAnalyticsId: String, customerAnalyticsId: String, regionCode: String) {
        publishEvent(SimProvisioningEvent(
                subscriptionAnalyticsId = subscriptionAnalyticsId,
                customerAnalyticsId = customerAnalyticsId,
                regionCode = regionCode.toLowerCase()
        ))
    }
}
