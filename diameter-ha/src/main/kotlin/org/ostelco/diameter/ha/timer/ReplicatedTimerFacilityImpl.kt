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
        var taskId = ""
        logger.debug("Schedule timer with timerName : $timerName sessionId : $sessionId delay : $delay")
        if ((sessionId != null) && (timerName != null)) {
            taskId = "$sessionId/$timerName"

            val data = ReplicatedTimerTaskData(taskId, sessionId, timerName,System.currentTimeMillis() + delay, -1)
            val timerTask = taskFactory.newTimerTask(data)
            replicatedTimerTaskScheduler.schedule(timerTask)
        } else {
            logger.warn("Can not schedule timer with sessionId $sessionId timerName $timerName")
        }
        return taskId
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