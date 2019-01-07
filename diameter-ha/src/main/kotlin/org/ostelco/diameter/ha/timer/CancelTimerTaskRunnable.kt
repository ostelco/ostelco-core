package org.ostelco.diameter.ha.timer

import org.ostelco.diameter.ha.logger

class CancelTimerTaskRunnable internal constructor(task: ReplicatedTimerTask,
                                                   scheduler: ReplicatedTimerTaskScheduler) : TimerTaskRunnable(task, scheduler) {

    private val logger by logger()

    override val type: Type
        get() = TimerTaskRunnable.Type.CANCEL


    override fun run() {

        logger.debug("Cancelling timer task for timer ID ${task.data.taskID}")

        scheduler.getLocalRunningTasksMap().remove(task.data.taskID)

        try {
            task.cancel()
        } catch (e: Throwable) {
            logger.error("Failed to cancel task ${task.data.taskID}", e)
        }
    }
}
