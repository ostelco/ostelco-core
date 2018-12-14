package org.ostelco.diameter.ha.timer

import org.ostelco.diameter.ha.logger

class CancelTimerAfterTxCommitRunnable internal constructor(task: ReplicatedTimerTask, scheduler: ReplicatedTimerTaskScheduler) : AfterTxCommitRunnable(task, scheduler) {

    override val type: Type
        get() = AfterTxCommitRunnable.Type.CANCEL


    override fun run() {

        val taskData = task.data
        val taskID = taskData.taskID

        if (logger.isDebugEnabled) {
            logger.info("Cancelling timer task for timer ID $taskID")
        }

        scheduler.getLocalRunningTasksMap().remove(taskID)

        try {
            task.cancel()
        } catch (e: Throwable) {
            logger.error(e.message, e)
        }

    }

    companion object {

        private val logger by logger()
    }

}
