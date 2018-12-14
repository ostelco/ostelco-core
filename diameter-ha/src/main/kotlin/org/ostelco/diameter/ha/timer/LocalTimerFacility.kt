package org.ostelco.diameter.ha.timer

import org.apache.commons.pool.BasePoolableObjectFactory
import org.apache.commons.pool.impl.GenericObjectPool
import org.jdiameter.client.api.IContainer
import org.jdiameter.client.impl.BaseSessionImpl
import org.jdiameter.common.api.concurrent.IConcurrentFactory
import org.jdiameter.common.api.data.ISessionDatasource
import org.jdiameter.common.api.timer.ITimerFacility
import org.jdiameter.common.impl.app.AppSessionImpl
import org.ostelco.diameter.ha.logger
import java.io.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

// Basically re-implementation of jdiameter/core/jdiameter/impl/src/main/java/org/jdiameter/common/impl/timer/LocalTimerFacilityImpl.java in Kotlin
// to get a grip on the functionallity.

class LocalTimerFacility(container: IContainer) : ITimerFacility {

    private val logger by logger()
    private val sessionDataSource: ISessionDatasource
    private val executor: ScheduledThreadPoolExecutor
    private val pool = GenericObjectPool(TimerTaskHandleFactory(), 100000, GenericObjectPool.WHEN_EXHAUSTED_GROW, 10, 20000)

    init {
        this.sessionDataSource = container.getAssemblerFacility().getComponentInstance(ISessionDatasource::class.java)
        this.executor = container.concurrentFactory.getScheduledExecutorService(IConcurrentFactory.ScheduledExecServices.ApplicationSession.name) as ScheduledThreadPoolExecutor
    }

    /**
     * This should schedule a timer do detect if session has timed out.
     */
    override fun schedule(sessionId: String?, timerName: String?, milliseconds: Long): Serializable {

        val id = "$sessionId/$timerName"
        logger.info("Scheduling timer with id: $id timerName: $timerName, mililseconds: $milliseconds")
        val timerTaskHandle = borrowTimerTaskHandle()
        timerTaskHandle!!.id = id
        timerTaskHandle.sessionId = sessionId
        timerTaskHandle.timerName = timerName
        timerTaskHandle.future = this.executor.schedule(timerTaskHandle, milliseconds, TimeUnit.MILLISECONDS)
        return timerTaskHandle
    }

    override fun cancel(timerTaskHandle: Serializable?) {
        if (timerTaskHandle != null && timerTaskHandle is TimerTaskHandle) {
            if (timerTaskHandle.future != null) {
                logger.info("Cancelling timer with id [{}] and delay [{}]", timerTaskHandle.id, timerTaskHandle.future!!.getDelay(TimeUnit.MILLISECONDS))
                if (executor.remove(timerTaskHandle.future as Runnable)) {
                    timerTaskHandle.future!!.cancel(false)
                    returnTimerTaskHandle(timerTaskHandle)
                }
            }
        }
    }


    private fun returnTimerTaskHandle(timerTaskHandle: TimerTaskHandle) {
        try {
            pool.returnObject(timerTaskHandle)
        } catch (e: Exception) {
            logger.warn(e.message)
        }
    }

    private fun borrowTimerTaskHandle(): TimerTaskHandle? {
        try {
            return pool.borrowObject() as TimerTaskHandle?
        } catch (e: Exception) {
            logger.error("", e)
        }
        return null
    }

    internal inner class TimerTaskHandleFactory : BasePoolableObjectFactory() {
        @Throws(Exception::class)
        override fun makeObject(): Any {
            return TimerTaskHandle()
        }

        @Throws(Exception::class)
        override fun passivateObject(obj: Any?) {
            var timerTaskHandle = obj as TimerTaskHandle?
            timerTaskHandle!!.id = null
            timerTaskHandle.sessionId = null
            timerTaskHandle.timerName = null
            timerTaskHandle.future = null
        }
    }

    private inner class TimerTaskHandle : Runnable, Externalizable {
        // its not really serializable;
        var sessionId: String? = null
        var timerName: String? = null
        var id: String? = null
        @Transient
        var future: ScheduledFuture<*>? = null

        override fun run() {
            try {
                val bSession = sessionDataSource.getSession(sessionId)
                if (bSession == null) {
                    // FIXME: error ?
                    logger.error("Base Session is null for sessionId: {}", sessionId)
                    return
                } else {
                    try {
                        if (!bSession.isAppSession) {
                            val impl = bSession as BaseSessionImpl
                            impl.onTimer(timerName!!)
                        } else {
                            val impl = bSession as AppSessionImpl
                            impl.onTimer(timerName)
                        }
                    } catch (e: Exception) {
                        logger.error("Caught exception from session object!", e)
                    }

                }
            } catch (e: Exception) {
                logger.error("Failure executing timer task with id: " + id!!, e)
            } finally {
                returnTimerTaskHandle(this)
            }
        }

        /*
     * (non-Javadoc)
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
        @Throws(IOException::class)
        override fun writeExternal(out: ObjectOutput) {
            logger.info("IMPLEMENT ME (writeExternal)")
            //throw IOException("Failed to serialize local timer!")
        }

        /*
     * (non-Javadoc)
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
        @Throws(IOException::class, ClassNotFoundException::class)
        override fun readExternal(`in`: ObjectInput) {
            logger.info("IMPLEMENT ME (readExternal)")
            //throw IOException("Failed to deserialize local timer!")
        }
    }
}

