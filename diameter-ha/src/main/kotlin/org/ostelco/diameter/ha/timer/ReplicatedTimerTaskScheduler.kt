package org.ostelco.diameter.ha.timer

import org.ostelco.diameter.ha.logger
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor

class ReplicatedTimerTaskScheduler {
    private val logger by logger()

    private val localRunningTasks: ConcurrentHashMap<Serializable, ReplicatedTimerTask> = ConcurrentHashMap();
    private val executor = ScheduledThreadPoolExecutor( 5, Executors.defaultThreadFactory())

    internal fun getExecutor(): ScheduledThreadPoolExecutor {
        return executor
    }

    internal fun getLocalRunningTasksMap(): ConcurrentHashMap<Serializable, ReplicatedTimerTask> {
        return localRunningTasks
    }

    internal fun remove(taskID: Serializable) {
        logger.debug("Remove taskID : $taskID")
        localRunningTasks.remove(taskID)
    }

    fun schedule(task: ReplicatedTimerTask, checkIfAlreadyPresent: Boolean) {
        logger.debug("Scheduling task with id ${task.data.taskID}")
        task.scheduler = this

        SetTimerAfterTxCommitRunnable(task, this).run()
    }

    fun cancel(taskID: Serializable): ReplicatedTimerTask? {

        logger.debug("Canceling task with timer id $taskID")


        val task: ReplicatedTimerTask? = localRunningTasks[taskID]
        if (task != null) {

            val setAction = task.action
            if (setAction != null) {
                // we have a tx action scheduled to run when tx commits, to set the timer, lets simply cancel it
                setAction.cancel()
            } else {
                // do cancellation
                val runnable = CancelTimerAfterTxCommitRunnable(task, this)
                runnable.run()
            }
        }
        return task
    }
}