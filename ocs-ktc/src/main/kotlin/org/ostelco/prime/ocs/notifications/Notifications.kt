package org.ostelco.prime.ocs.notifications

import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.module.getResource
import org.ostelco.prime.ocs.ConfigRegistry

object Notifications {

    private val appNotifier by lazy { getResource<AppNotifier>() }

    fun lowBalanceAlert(msisdn: String, reserved: Long, balance: Long) {
        val lowBalanceThreshold = ConfigRegistry.config.lowBalanceThreshold
        if ((balance < lowBalanceThreshold) && ((balance + reserved) > lowBalanceThreshold)) {
            appNotifier.notify(msisdn, "Pi", "You have less then " + lowBalanceThreshold / 1000000 + "Mb data left")
        }
    }
}