package org.ostelco.diameter.ha.sessiondatafactory

import org.jdiameter.api.app.AppSession
import org.jdiameter.api.cca.ClientCCASession
import org.jdiameter.api.cca.ServerCCASession
import org.jdiameter.common.api.app.IAppSessionDataFactory
import org.jdiameter.common.api.app.cca.ICCASessionData
import org.jdiameter.common.api.data.ISessionDatasource
import org.ostelco.diameter.ha.client.ClientCCASessionDataReplicatedImpl
import org.ostelco.diameter.ha.server.ServerCCASessionDataReplicatedImpl
import org.ostelco.diameter.ha.sessiondatasource.RedisReplicatedSessionDatasource

class CCARedisReplicatedSessionDataFactory

(replicatedSessionDataSource: ISessionDatasource) : IAppSessionDataFactory<ICCASessionData> {

    private val replicatedSessionDataSource: RedisReplicatedSessionDatasource

    init {
        this.replicatedSessionDataSource = replicatedSessionDataSource as RedisReplicatedSessionDatasource
    }

    override fun getAppSessionData(clazz: Class<out AppSession>, sessionId: String): ICCASessionData {
        if (clazz == ClientCCASession::class.java) {
            val data = ClientCCASessionDataReplicatedImpl(sessionId)
            return data
        } else if (clazz == ServerCCASession::class.java) {
            val data = ServerCCASessionDataReplicatedImpl(sessionId)
            return data
        }
        throw IllegalArgumentException(clazz.toString())
    }
}