package org.ostelco.prime.analytics.publishers

import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.analytics.events.SimProvisioningEvent
import org.ostelco.common.publisherex.DelegatePubSubPublisher
import org.ostelco.common.publisherex.PubSubPublisher

/**
 * This holds logic for sending SIM provisioning events to Cloud Pub/Sub.
 */
object SimProvisioningPublisher :
        PubSubPublisher by DelegatePubSubPublisher(
                topicId = ConfigRegistry.config.simProvisioningTopicId,
                projectId = ConfigRegistry.config.projectId) {

    fun publish(subscriptionAnalyticsId: String, customerAnalyticsId: String, regionCode: String) {
        publishEvent(SimProvisioningEvent(
                subscriptionAnalyticsId = subscriptionAnalyticsId,
                customerAnalyticsId = customerAnalyticsId,
                regionCode = regionCode.toLowerCase()
        ))
    }
}
