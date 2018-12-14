package org.ostelco.diameter.ha.timer

import org.jdiameter.client.impl.BaseSessionImpl
import org.jdiameter.common.api.data.ISessionDatasource
import org.jdiameter.common.impl.app.AppSessionImpl
import org.ostelco.diameter.ha.logger
import java.io.Serializable
import java.util.concurrent.ScheduledFuture

class ReplicatedTimerTaskData(val taskID: Serializable, val sessionId: String, val timerName: String, var delay: Long, var startTime: Long, var period: Long) : Serializable {

    override fun hashCode(): Int {
        return taskID.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other != null && other.javaClass == this.javaClass) (other as ReplicatedTimerTaskData).taskID == this.taskID else false
    }

    companion object {
        private const val serialVersionUID = 8774218122384404226L
    }
}

class ReplicatedTimerTask(val data: ReplicatedTimerTaskData, val sessionDataSource: ISessionDatasource) : Runnable {

    var action: SetTimerAfterTxCommitRunnable? = null

    var scheduledFuture: ScheduledFuture<*>? = null
        set(scheduledFuture) {
            field = scheduledFuture
            if (cancel) {
                scheduledFuture!!.cancel(false)
            }

        }

    var scheduler: ReplicatedTimerTaskScheduler? = null
    protected var autoRemoval = true
    @Transient
    private var cancel: Boolean = false


    fun cancel() {
        cancel = true
        if (scheduledFuture != null) {
            scheduledFuture!!.cancel(false)
        }

    }

    override fun run() {
        if (data.period < 0L && autoRemoval) {
            logger.info("Task with id ${data.taskID}  is not recurring, so removing it locally and in the cluster")
            removeFromScheduler()
        } else {
            logger.info("Task with id ${data.taskID} is recurring, not removing it locally nor in the cluster")
        }

        logger.info("Firing Timer with id ${data.taskID}")

        runTask()
    }

    protected fun removeFromScheduler() {
        scheduler!!.remove(data.taskID)
    }

    fun runTask() {
        try {
            val bSession = sessionDataSource.getSession(data.sessionId)
            if (bSession == null) {
                logger.error("Base Session is null for sessionId: ${data.sessionId}")
                return
            } else {
                try {
                    if (bSession.isAppSession()) {
                        val impl = bSession as BaseSessionImpl
                        impl.onTimer(data.timerName)
                    } else {
                        val impl = bSession as AppSessionImpl
                        impl.onTimer(data.timerName)
                    }
                } catch (e: Exception) {
                    logger.error("Caught exception from session object!", e)
                }

            }
        } catch (e: Exception) {
            logger.error("Failure executing timer task", e)
        }

    }

    fun beforeRecover() {
        if (data.period > 0L) {
            val now = System.currentTimeMillis()
            var startTime = data.startTime

            while (startTime <= now) {
                startTime += data.startTime
                data.startTime = startTime
            }

            logger.info("Task with id ${data.taskID} start time reset to ${data.startTime}")
        }

    }

    companion object {
        private val logger by logger()
    }
}