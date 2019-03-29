package org.ostelco.prime.customer.endpoint.metrics

import org.ostelco.prime.analytics.AnalyticsService
import org.ostelco.prime.analytics.PrimeMetric.TOTAL_USERS
import org.ostelco.prime.analytics.PrimeMetric.USERS_ACQUIRED_THROUGH_REFERRALS
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource

val analyticsService: AnalyticsService = getResource()
val adminStore: AdminDataSource = getResource()

fun reportMetricsAtStartUp() {
    analyticsService.reportMetric(TOTAL_USERS, adminStore.getCustomerCount())
    analyticsService.reportMetric(USERS_ACQUIRED_THROUGH_REFERRALS, adminStore.getReferredCustomerCount())
}

fun updateMetricsOnNewSubscriber() {
    analyticsService.reportMetric(TOTAL_USERS, adminStore.getCustomerCount())
    analyticsService.reportMetric(USERS_ACQUIRED_THROUGH_REFERRALS, adminStore.getReferredCustomerCount())
}