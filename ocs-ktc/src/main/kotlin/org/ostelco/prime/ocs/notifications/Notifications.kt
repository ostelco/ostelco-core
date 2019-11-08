package org.ostelco.prime.ocs.notifications

import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.module.getResource
import org.ostelco.prime.ocs.ConfigRegistry
import org.ostelco.prime.storage.AdminDataSource

object Notifications {

    private val appNotifier by lazy { getResource<AppNotifier>() }
    private val storage by lazy { getResource<AdminDataSource>() }

    fun lowBalanceAlert(msisdn: String, reserved: Long, balance: Long) {
        val lowBalanceThreshold = ConfigRegistry.config.lowBalanceThreshold
        if ((balance < lowBalanceThreshold) && ((balance + reserved) > lowBalanceThreshold)) {
            // TODO martin : Title and message should differ depending on subscription
            storage.getCustomersForMsisdn(msisdn).map { customers ->
                customers.forEach { customer ->
                    appNotifier.notify(customer.id, "OYA", "You have less then " + lowBalanceThreshold / 1000000 + "Mb data left")
                }
            }
        }
    }
}