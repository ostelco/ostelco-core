package org.ostelco.diameter.ha.server

import org.jdiameter.client.api.IContainer
import org.jdiameter.common.api.app.cca.ServerCCASessionState
import org.jdiameter.server.impl.app.cca.IServerCCASessionData
import org.ostelco.diameter.ha.common.AppSessionDataRedisReplicatedImpl
import org.ostelco.diameter.ha.common.RedisStorage
import org.ostelco.diameter.ha.logger
import java.io.*
import java.lang.IllegalStateException
import java.util.*

class ServerCCASessionDataRedisReplicatedImpl(id: String, redisStorage: RedisStorage, container: IContainer) : AppSessionDataRedisReplicatedImpl(id, redisStorage), IServerCCASessionData {

    private val logger by logger()

    private val TCCID = "TCCID"
    private val STATELESS = "STATELESS"
    private val STATE = "STATE"

    init {
        setServerCCASessionState(ServerCCASessionState.IDLE)
    }

    override fun isStateless(): Boolean {
        return toPrimitive(redisStorage.getValue(id, STATELESS), true)
    }

    override fun setStateless(stateless: Boolean) {
        storeValue(STATELESS, stateless.toString())
    }

    override fun getServerCCASessionState(): ServerCCASessionState {
        val value = redisStorage.getValue(id, STATE)
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
        storeValue(TCCID, toBase64String(tccTimerId))
    }

    override fun getTccTimerId(): Serializable {
        val value = redisStorage.getValue(id, TCCID)
        if (value != null) {
            return fromBase64String(value)
        } else {
            throw IllegalStateException()
        }
    }
}