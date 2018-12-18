package org.ostelco.diameter.ha.timer

import org.ostelco.diameter.ha.logger

class CancelTimerAfterTxCommitRunnable internal constructor(task: ReplicatedTimerTask,
                                                            scheduler: ReplicatedTimerTaskScheduler) : AfterTxCommitRunnable(task, scheduler) {

    private val logger by logger()

    override val type: Type
        get() = AfterTxCommitRunnable.Type.CANCEL


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
