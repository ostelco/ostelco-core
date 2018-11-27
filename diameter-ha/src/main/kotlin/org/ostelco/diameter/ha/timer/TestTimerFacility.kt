package org.ostelco.diameter.ha.timer

import org.jdiameter.common.api.timer.ITimerFacility
import java.io.Serializable

public class TestTimerFacility : ITimerFacility {
    override fun schedule(sessionId: String?, timerName: String?, miliseconds: Long): Serializable {
        TODO("not implemented")
    }

    override fun cancel(id: Serializable?) {
        TODO("not implemented")
    }
}