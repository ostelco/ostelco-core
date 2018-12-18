package org.ostelco.diameter.ha.timer

import org.jdiameter.client.api.IContainer
import org.jdiameter.common.api.data.ISessionDatasource
import org.jdiameter.common.api.timer.ITimerFacility
import org.ostelco.diameter.ha.logger
import java.io.Serializable

class ReplicatedTimerFacilityImpl(container: IContainer) : ITimerFacility {

    private val logger by logger()
    private val sessionDataSource: ISessionDatasource = container.assemblerFacility.getComponentInstance(ISessionDatasource::class.java)
    private val taskFactory: TimerTaskFactory = TimerTaskFactory()
    private val replicatedTimerTaskScheduler: ReplicatedTimerTaskScheduler = ReplicatedTimerTaskScheduler()

    override fun schedule(sessionId: String?, timerName: String?, delay: Long): Serializable {
        logger.debug(" schedule sessionId : $sessionId timerName : $timerName delay : $delay")
        if ((sessionId != null) && (timerName != null)) {
            val taskId = "$sessionId/$timerName"
            logger.debug("Scheduling timer task with id $taskId")

            //        if (replicatedTimerTaskScheduler.getTimerTaskData(id) != null) {
            //            throw IllegalArgumentException("Timer already running: $id")
            //        }

            val data = ReplicatedTimerTaskData(taskId, sessionId, timerName, delay, System.currentTimeMillis() + delay, -1)
            val timerTask = taskFactory.newTimerTask(data)
            replicatedTimerTaskScheduler.schedule(timerTask, true)
            return taskId
        } else {
            logger.error("sessionId $sessionId timerName $timerName")
            return ""
        }
    }

    override fun cancel(id: Serializable?) {
        logger.debug("Cancelling timer with id $id")
        if (id != null) {
            replicatedTimerTaskScheduler.cancel(id)
        }
    }

    private inner class TimerTaskFactory {

        fun newTimerTask(data: ReplicatedTimerTaskData): ReplicatedTimerTask {
            return ReplicatedTimerTask(data, sessionDataSource)
        }
    }

}