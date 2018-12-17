package org.ostelco.diameter.ha.timer

import org.jdiameter.client.api.IContainer
import org.jdiameter.common.api.data.ISessionDatasource
import org.jdiameter.common.api.timer.ITimerFacility
import org.ostelco.diameter.ha.logger
import java.io.Serializable

class ReplicatedTimerFacilityImpl(container: IContainer) : ITimerFacility {

    private val logger by logger()
    private val sessionDataSource: ISessionDatasource
    private val taskFactory: TimerTaskFactory;
    private val replicatedTimerTaskScheduler: ReplicatedTimerTaskScheduler

    init {
        this.sessionDataSource = container.assemblerFacility.getComponentInstance(ISessionDatasource::class.java)
        this.taskFactory = TimerTaskFactory()
        this.replicatedTimerTaskScheduler = ReplicatedTimerTaskScheduler()
    }

    override fun schedule(sessionId: String?, timerName: String?, delay: Long): Serializable {
        logger.info("sessionId : $sessionId")
        logger.info("timerName : $timerName")
        if ((sessionId != null) && (timerName != null)) {
            val id = "$sessionId/$timerName"
            logger.info("Scheduling timer with id $id")

            //        if (this.replicatedTimerTaskScheduler.getTimerTaskData(id) != null) {
            //            throw IllegalArgumentException("Timer already running: $id")
            //        }

            val data = ReplicatedTimerTaskData(id, sessionId, timerName, delay, System.currentTimeMillis() + delay, -1)
            val timerTask = this.taskFactory.newTimerTask(data)
            replicatedTimerTaskScheduler.schedule(timerTask, true)
            return id
        } else {
            logger.error("sessionId $sessionId timerName $timerName")
            return ""
        }
    }

    override fun cancel(id: Serializable?) {
        logger.info("Cancelling timer with id $id")
        if (id != null) {
            this.replicatedTimerTaskScheduler.cancel(id)
        }
    }

    private inner class TimerTaskFactory {

        fun newTimerTask(data: ReplicatedTimerTaskData): ReplicatedTimerTask {
            return ReplicatedTimerTask(data, sessionDataSource)
        }
    }

}