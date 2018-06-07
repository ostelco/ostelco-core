package org.ostelco.prime.thresholds

import com.lmax.disruptor.EventHandler
import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.disruptor.PrimeEvent
import org.ostelco.prime.disruptor.PrimeEventMessageType.CREDIT_CONTROL_REQUEST
import org.ostelco.prime.logger
import org.ostelco.prime.module.getResource

/**
 * This class will check if we should send notification based on updated balance
 */
class ThresholdChecker(private val lowBalanceThreshold: Long) : EventHandler<PrimeEvent> {

    private val LOG by logger()

    private val appNotifier by lazy { getResource<AppNotifier>() }
     // private val appNotifier by getResource<AppNotifier>()

    override fun onEvent(
            event: PrimeEvent,
            sequence: Long,
            endOfBatch: Boolean) {

        if (event.messageType != CREDIT_CONTROL_REQUEST) {
            return
        }

        checkThreshold(event);
    }

    private fun checkThreshold(event: PrimeEvent) {
        // Check that we just crossed the threshold
        if ((event.bundleBytes < lowBalanceThreshold) && ((event.bundleBytes + event.reservedBucketBytes) > lowBalanceThreshold)) {
            val msisdn = event.msisdn;
            if (msisdn != null) {
                appNotifier.notify(msisdn);
            }
        }
    }
}
