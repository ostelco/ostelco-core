package org.ostelco.diameter.ha.server

import org.jdiameter.common.api.app.cca.ServerCCASessionState
import org.jdiameter.server.impl.app.cca.IServerCCASessionData
import org.ostelco.diameter.ha.common.AppSessionDataRedisReplicatedImpl
import java.io.Serializable

class ServerCCASessionDataReplicatedImpl(sessionId: String) : AppSessionDataRedisReplicatedImpl(), IServerCCASessionData {
    override fun isStateless(): Boolean {
        TODO("not implemented")
    }

    override fun getServerCCASessionState(): ServerCCASessionState {
        TODO("not implemented")
    }

    override fun getTccTimerId(): Serializable {
        TODO("not implemented")
    }

    override fun setServerCCASessionState(state: ServerCCASessionState?) {
        TODO("not implemented")
    }

    override fun setStateless(stateless: Boolean) {
        TODO("not implemented")
    }

    override fun setTccTimerId(tccTimerId: Serializable?) {
        TODO("not implemented")
    }
}