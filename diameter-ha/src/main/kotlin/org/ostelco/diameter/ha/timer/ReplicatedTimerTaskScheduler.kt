package org.ostelco.diameter.ha.timer

import org.ostelco.diameter.ha.logger
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor

class ReplicatedTimerTaskScheduler {
    private val logger by logger()

    private val localRunningTasks: ConcurrentHashMap<Serializable, ReplicatedTimerTask> = ConcurrentHashMap()
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

    fun schedule(task: ReplicatedTimerTask) {
        task.setScheduler(this)
        SetTimerTaskRunnable(task, this).run()
    }

    fun cancel(taskID: Serializable): ReplicatedTimerTask? {

        logger.debug("Cancelling task with timer id $taskID")

        val task: ReplicatedTimerTask? = localRunningTasks[taskID]
        if (task != null) {
            CancelTimerTaskRunnable(task, this).run()
        }
        return task
    }
}