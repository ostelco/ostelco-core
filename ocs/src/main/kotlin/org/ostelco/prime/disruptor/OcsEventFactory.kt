package org.ostelco.prime.disruptor

import com.lmax.disruptor.EventFactory

class OcsEventFactory : EventFactory<OcsEvent> {

    override fun newInstance(): OcsEvent {
        return OcsEvent()
    }
}
