package org.ostelco.diameter.ha.timer

abstract class AfterTxCommitRunnable(protected val task: ReplicatedTimerTask,
                                     protected val scheduler: ReplicatedTimerTaskScheduler) : Runnable {

    abstract val type: Type

    enum class Type {
        SET, CANCEL
    }
}
