package org.ostelco.diameter.ha.timer

import org.jdiameter.client.api.IContainer
import org.jdiameter.common.api.data.ISessionDatasource
import org.jdiameter.common.api.timer.ITimerFacility
import org.ostelco.diameter.ha.logger
import java.io.Serializable

// Basically re-implementation of jdiameter/core/jdiameter/impl/src/main/java/org/jdiameter/common/impl/timer/LocalTimerFacilityImpl.java
// to get a grip on the functionallity.

class LocalTimerFacility(container: IContainer) : ITimerFacility {

    private val logger by logger()
    private val sessionDataSource: ISessionDatasource

    init {
        this.sessionDataSource = container.getAssemblerFacility().getComponentInstance(ISessionDatasource::class.java)
    }

    /**
     * This should schedule a timer do detect if session has timed out.
     */
    override fun schedule(sessionId: String?, timerName: String?, miliseconds: Long): Serializable {
        val id = "$sessionId/$timerName"
        logger.debug("Scheduling timer with id: $id timerName: $timerName, mililseconds: $miliseconds")
        return id
    }

    override fun cancel(id: Serializable?) {
        logger.debug("Cancel timer with id: $id")
    }
}

