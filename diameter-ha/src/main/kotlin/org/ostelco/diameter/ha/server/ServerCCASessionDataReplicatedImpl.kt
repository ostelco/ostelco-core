package org.ostelco.diameter.ha.server

import org.jdiameter.api.cca.ServerCCASession
import org.jdiameter.common.api.app.cca.ServerCCASessionState
import org.jdiameter.server.impl.app.cca.IServerCCASessionData
import org.ostelco.diameter.ha.common.AppSessionDataReplicatedImpl
import org.ostelco.diameter.ha.common.ReplicatedStorage
import java.io.*
import java.lang.IllegalStateException

class ServerCCASessionDataReplicatedImpl(sessionId: String, replicatedStorage: ReplicatedStorage) : AppSessionDataReplicatedImpl(sessionId, replicatedStorage), IServerCCASessionData {

    private val TCCID = "TCCID"
    private val STATELESS = "STATELESS"
    private val STATE = "STATE"

    init {
        if (!replicatedStorage.exist(sessionId)) {
            setAppSessionIface(ServerCCASession::class.java)
            serverCCASessionState = ServerCCASessionState.IDLE
        }
    }

    override fun isStateless(): Boolean {
        return toPrimitive(getValue(STATELESS), true)
    }

    override fun setStateless(stateless: Boolean) {
        storeValue(STATELESS, stateless.toString())
    }

    override fun getServerCCASessionState(): ServerCCASessionState {
        val value = getValue(STATE)
        if (value != null) {
            return ServerCCASessionState.valueOf(value)
        } else {
            throw IllegalStateException()
        }
    }

    override fun setServerCCASessionState(state: ServerCCASessionState?) {
        storeValue(STATE, state.toString())
    }

    override fun setTccTimerId(tccTimerId: Serializable?) {
        if (tccTimerId != null) {
            storeValue(TCCID, toBase64String(tccTimerId))
        }
    }

    override fun getTccTimerId(): Serializable? {
        val value = getValue(TCCID)
        if (value != null) {
            return fromBase64String(value)
        } else {
            return value
        }
    }
}