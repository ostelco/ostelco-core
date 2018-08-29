package org.ostelco.prime.client.api.metrics

import org.ostelco.prime.analytics.AnalyticsService
import org.ostelco.prime.analytics.PrimeMetric.TOTAL_USERS
import org.ostelco.prime.analytics.PrimeMetric.USERS_ACQUIRED_THROUGH_REFERRALS
import org.ostelco.prime.analytics.PrimeMetric.USERS_PAID_AT_LEAST_ONCE
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource

val analyticsService: AnalyticsService = getResource()
val adminStore: AdminDataSource = getResource()

fun reportMetricsAtStartUp() {
    analyticsService.reportMetric(TOTAL_USERS, adminStore.getSubscriberCount())
    analyticsService.reportMetric(USERS_ACQUIRED_THROUGH_REFERRALS, adminStore.getReferredSubscriberCount())
    analyticsService.reportMetric(USERS_PAID_AT_LEAST_ONCE, adminStore.getPaidSubscriberCount())
}

fun updateMetricsOnNewSubscriber() {
    analyticsService.reportMetric(TOTAL_USERS, adminStore.getSubscriberCount())
    analyticsService.reportMetric(USERS_ACQUIRED_THROUGH_REFERRALS, adminStore.getReferredSubscriberCount())
}
fun updateMetricsOnPurchase() {
    analyticsService.reportMetric(USERS_PAID_AT_LEAST_ONCE, adminStore.getPaidSubscriberCount())
}