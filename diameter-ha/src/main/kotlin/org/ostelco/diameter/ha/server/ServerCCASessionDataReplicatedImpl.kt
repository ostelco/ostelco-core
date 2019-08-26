package org.ostelco.diameter.ha.server

import org.jdiameter.api.cca.ServerCCASession
import org.jdiameter.common.api.app.cca.ServerCCASessionState
import org.jdiameter.server.impl.app.cca.IServerCCASessionData
import org.ostelco.diameter.ha.common.AppSessionDataReplicatedImpl
import org.ostelco.diameter.ha.common.ReplicatedStorage
import org.ostelco.diameter.ha.logger
import java.io.*
import java.lang.IllegalStateException

class ServerCCASessionDataReplicatedImpl(sessionId: String, replicatedStorage: ReplicatedStorage) : AppSessionDataReplicatedImpl(sessionId, replicatedStorage), IServerCCASessionData {

    private val TCCID = "TCCID"
    private val STATELESS = "STATELESS"
    private val STATE = "STATE"

    private val logger by logger()

    private val localStoredState:HashMap<String,Any?> = HashMap()

    init {
        if (!replicatedStorage.exist(sessionId)) {
            setAppSessionIface(ServerCCASession::class.java)
            serverCCASessionState = ServerCCASessionState.IDLE
        }
    }

    override fun isStateless(): Boolean {

        if (localStoredState.containsKey(STATELESS)) {
            return localStoredState.get(STATELESS) as Boolean
        }

        return toPrimitive(getValue(STATELESS), true)
    }

    override fun setStateless(stateless: Boolean) {
        localStoredState.put(STATELESS, stateless)
        storeValue(STATELESS, stateless.toString())
    }

    override fun getServerCCASessionState(): ServerCCASessionState {

        if (localStoredState.containsKey(STATE)) {
            return localStoredState.get(STATE) as ServerCCASessionState
        }

        val value = getValue(STATE)
        if (value != null) {
            return ServerCCASessionState.valueOf(value)
        } else {
            logger.warn("Failed to fetch STATE for session [$sessionId]")
            throw IllegalStateException()
        }
    }

    override fun setServerCCASessionState(state: ServerCCASessionState?) {

        localStoredState.put(STATE, state)
        storeValue(STATE, state.toString())
    }

    override fun setTccTimerId(tccTimerId: Serializable?) {
        
        localStoredState.put(TCCID, tccTimerId)
        if (tccTimerId != null) {
            storeValue(TCCID, toBase64String(tccTimerId))
        }
    }

    override fun getTccTimerId(): Serializable? {

        if (localStoredState.containsKey(TCCID)) {
            return localStoredState.get(TCCID) as Serializable?
        }

        val value = getValue(TCCID)
        if (value != null) {
            return fromBase64String(value)
        } else {
            return value
        }
    }
}