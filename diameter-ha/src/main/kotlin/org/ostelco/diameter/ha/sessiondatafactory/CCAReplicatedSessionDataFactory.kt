package org.ostelco.diameter.ha.sessiondatafactory

import org.jdiameter.api.app.AppSession
import org.jdiameter.api.cca.ClientCCASession
import org.jdiameter.api.cca.ServerCCASession
import org.jdiameter.common.api.app.IAppSessionDataFactory
import org.jdiameter.common.api.app.cca.ICCASessionData
import org.jdiameter.common.api.data.ISessionDatasource
import org.ostelco.diameter.ha.client.ClientCCASessionDataReplicatedImpl
import org.ostelco.diameter.ha.common.ReplicatedStorage
import org.ostelco.diameter.ha.server.ServerCCASessionDataReplicatedImpl
import org.ostelco.diameter.ha.sessiondatasource.RedisReplicatedSessionDatasource

class CCAReplicatedSessionDataFactory(replicatedSessionDataSource: ISessionDatasource) : IAppSessionDataFactory<ICCASessionData> {

    private val replicatedSessionDataSource: RedisReplicatedSessionDatasource
    private val replicatedStorage: ReplicatedStorage

    init {
        this.replicatedSessionDataSource = replicatedSessionDataSource as RedisReplicatedSessionDatasource
        this.replicatedStorage = replicatedSessionDataSource.getReplicatedStorage()
    }

    override fun getAppSessionData(clazz: Class<out AppSession>, sessionId: String): ICCASessionData {

        if (clazz == ClientCCASession::class.java) {
            val data = ClientCCASessionDataReplicatedImpl(sessionId, this.replicatedStorage, this.replicatedSessionDataSource.container)
            return data
        } else if (clazz == ServerCCASession::class.java) {
            val data = ServerCCASessionDataReplicatedImpl(sessionId, this.replicatedStorage)
            return data
        }
        throw IllegalArgumentException(clazz.toString())
    }
}