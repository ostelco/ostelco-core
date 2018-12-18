package org.ostelco.diameter.ha.timer

import org.ostelco.diameter.ha.logger
import java.util.concurrent.TimeUnit

class SetTimerAfterTxCommitRunnable(task: ReplicatedTimerTask,
                                    scheduler: ReplicatedTimerTaskScheduler) : AfterTxCommitRunnable(task, scheduler) {
    private val logger by logger()

    private var canceled = false

    override val type: Type
         get() = Type.SET

    override fun run() {
        task.action = null
        if (!canceled) {
            logger.debug("SetTimerAfterTxCommitRunnable run")
            val previousTask = scheduler.getLocalRunningTasksMap().putIfAbsent(task.data.taskID, task)
            if (previousTask != null) {
                logger.debug("A task with id ${task.data.taskID} has already been added to the local tasks, not rescheduling")
            }

            val taskData = task.data
            var delay = taskData.startTime - System.currentTimeMillis()
            if (delay < 0L) {
                delay = 0L
            }

            try {
                if (taskData.period < 0L) {
                    logger.debug("Scheduling one-shot timer with id ${task.data.taskID} , delay $delay")

                    task.scheduledFuture = scheduler.getExecutor().schedule(task, delay, TimeUnit.MILLISECONDS)
                } else {
                    logger.debug("Scheduling periodic timer with id ${task.data.taskID}, delay $delay period ${taskData.period}")

                    task.scheduledFuture = scheduler.getExecutor().scheduleWithFixedDelay(task, delay, taskData.period, TimeUnit.MILLISECONDS)
                }
            } catch (t: Throwable) {
                logger.error("Failed to schedule task with id ${taskData.taskID}", t)
                scheduler.remove(taskData.taskID)
            }

        } else {
            logger.debug("Canceled scheduling periodic timer with id : ${task.data.taskID}")
        }
    }

    fun cancel() {
        logger.debug("Canceling set timer action for task with timer id ${task.data.taskID}")

        canceled = true
        scheduler.remove(task.data.taskID)
    }
}
