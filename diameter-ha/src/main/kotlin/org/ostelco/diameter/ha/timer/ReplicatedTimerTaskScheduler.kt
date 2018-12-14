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
        return this.executor
    }

    internal fun getLocalRunningTasksMap(): ConcurrentHashMap<Serializable, ReplicatedTimerTask> {
        return this.localRunningTasks
    }

    internal fun remove(taskID: Serializable) {
        logger.info("Remove taskID : $taskID")
        this.localRunningTasks.remove(taskID)
    }

    fun schedule(task: ReplicatedTimerTask, checkIfAlreadyPresent: Boolean) {
        logger.info("Scheduling task with id ${task.data.taskID}")
        task.scheduler = this

        SetTimerAfterTxCommitRunnable(task, this).run()
    }

    fun cancel(taskID: Serializable): ReplicatedTimerTask? {

        logger.info("Canceling task with timer id $taskID")


        var task: ReplicatedTimerTask? = localRunningTasks[taskID]
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