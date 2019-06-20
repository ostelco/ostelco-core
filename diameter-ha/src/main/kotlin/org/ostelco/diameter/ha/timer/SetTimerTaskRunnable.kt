package org.ostelco.diameter.ha.timer

import org.ostelco.diameter.ha.logger
import java.util.concurrent.TimeUnit

class SetTimerTaskRunnable(task: ReplicatedTimerTask,
                           scheduler: ReplicatedTimerTaskScheduler) : TimerTaskRunnable(task, scheduler) {
    private val logger by logger()

    override val type: Type
         get() = Type.SET

    override fun run() {
        val previousTask = scheduler.getLocalRunningTasksMap().putIfAbsent(task.data.taskID, task)
        if (previousTask != null) {
            logger.debug("A task with id ${task.data.taskID} has already been added to the local tasks, not rescheduling")
            return
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
    }
}
