package org.ostelco.diameter.ha.timer

import org.ostelco.diameter.ha.logger
import java.util.concurrent.TimeUnit

class SetTimerAfterTxCommitRunnable(task: ReplicatedTimerTask, scheduler: ReplicatedTimerTaskScheduler) : AfterTxCommitRunnable(task, scheduler) {
    private var canceled = false

    override val type: Type
         get() = Type.SET

    override fun run() {
        task.action = null
        if (!canceled) {
            logger.info("run")
            val previousTask = scheduler.getLocalRunningTasksMap().putIfAbsent(task.data.taskID, task)
            if (previousTask != null) {
                logger.info("A task with id ${task.data.taskID} has already been added to the local tasks, not rescheduling")
            }

            val taskData = task.data
            var delay = taskData.startTime - System.currentTimeMillis()
            if (delay < 0L) {
                delay = 0L
            }

            try {
                if (taskData.period < 0L) {
                    logger.info("Scheduling one-shot timer with id ${task.data.taskID} , delay $delay")

                    task.scheduledFuture = this.scheduler.getExecutor().schedule(this.task, delay, TimeUnit.MILLISECONDS)
                } else {
                    logger.info("Scheduling periodic timer with id ${task.data.taskID}, delay $delay period ${taskData.period}")

                    task.scheduledFuture = this.scheduler.getExecutor().scheduleWithFixedDelay(task, delay, taskData.period, TimeUnit.MILLISECONDS)
                }
            } catch (t: Throwable) {
                logger.error(t.message, t)
                scheduler.remove(taskData.taskID)
            }

        } else {
            logger.info("Canceled scheduling periodic timer with id : ${task.data.taskID}")
        }

    }

    fun cancel() {
        logger.info("Canceling set timer action for task with timer id ${task.data.taskID}")

        canceled = true
        scheduler.remove(task.data.taskID)
    }

    companion object {
        private val logger by logger()
    }
}
