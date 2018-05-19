package org.ostelco.prime.disruptor

import com.lmax.disruptor.EventFactory

class PrimeEventFactory : EventFactory<PrimeEvent> {

    override fun newInstance(): PrimeEvent {
        return PrimeEvent()
    }
}
