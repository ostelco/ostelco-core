package org.ostelco.prime.thresholds

import com.lmax.disruptor.EventHandler
import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.disruptor.EventMessageType.CREDIT_CONTROL_REQUEST
import org.ostelco.prime.disruptor.OcsEvent
import org.ostelco.prime.module.getResource

/**
 * This class will check if we should send notification based on updated balance
 */
class ThresholdChecker(private val lowBalanceThreshold: Long) : EventHandler<OcsEvent> {

    private val appNotifier by lazy { getResource<AppNotifier>() }

    override fun onEvent(
            event: OcsEvent,
            sequence: Long,
            endOfBatch: Boolean) {

        if (event.messageType != CREDIT_CONTROL_REQUEST) {
            return
        }

        checkThreshold(event)
    }

    private fun checkThreshold(event: OcsEvent) {
        // Check that we just crossed the threshold
        if ((event.bundleBytes < lowBalanceThreshold) && ((event.bundleBytes + event.reservedBucketBytes) > lowBalanceThreshold)) {
            val msisdn = event.msisdn
            if (msisdn != null) {
                appNotifier.notify(msisdn, "Pi", "You have less then " + lowBalanceThreshold/1000000 + "Mb data left")
            }
        }
    }
}
