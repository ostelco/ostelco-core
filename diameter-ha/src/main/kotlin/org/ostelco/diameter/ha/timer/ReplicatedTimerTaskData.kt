package org.ostelco.diameter.ha.timer

import org.jdiameter.client.impl.BaseSessionImpl
import org.jdiameter.common.api.data.ISessionDatasource
import org.jdiameter.common.impl.app.AppSessionImpl
import org.ostelco.diameter.ha.logger
import java.io.Serializable
import java.util.concurrent.ScheduledFuture

class ReplicatedTimerTaskData(val taskID: Serializable,
                              val sessionId: String,
                              val timerName: String,
                              var startTime: Long,
                              var period: Long) : Serializable {

    override fun hashCode(): Int {
        return taskID.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other != null && other.javaClass == this.javaClass) (other as ReplicatedTimerTaskData).taskID == taskID else false
    }

    companion object {
        private const val serialVersionUID = 8774218122384404226L
    }
}

class ReplicatedTimerTask(val data: ReplicatedTimerTaskData, private val sessionDataSource: ISessionDatasource) : Runnable {

    private val logger by logger()

    var scheduledFuture: ScheduledFuture<*>? = null
        set(scheduledFuture) {
            field = scheduledFuture
            if (cancel) {
                scheduledFuture!!.cancel(false)
            }
        }

    var scheduler: ReplicatedTimerTaskScheduler? = null
    private var autoRemoval = true
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
            logger.debug("Task with id ${data.taskID} is not recurring, so removing it")
            removeFromScheduler()
        } else {
            logger.debug("Task with id ${data.taskID} is recurring, not removing it")
        }

        logger.debug("Firing Timer with id ${data.taskID}")

        runTask()
    }

    private fun removeFromScheduler() {
        scheduler!!.remove(data.taskID)
    }

    private fun runTask() {
        try {
            val bSession = sessionDataSource.getSession(data.sessionId)
            if (bSession == null) {
                logger.error("Base Session is null for sessionId: ${data.sessionId}")
                return
            } else {
                try {
                    if (!bSession.isAppSession) {
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
}