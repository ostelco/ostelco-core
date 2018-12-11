package org.ostelco.diameter.ha.sessiondatafactory

import org.jdiameter.api.app.AppSession
import org.jdiameter.api.cca.ClientCCASession
import org.jdiameter.api.cca.ServerCCASession
import org.jdiameter.common.api.app.IAppSessionDataFactory
import org.jdiameter.common.api.app.cca.ICCASessionData
import org.jdiameter.common.api.data.ISessionDatasource
import org.ostelco.diameter.ha.client.ClientCCASessionDataRedisReplicatedImpl
import org.ostelco.diameter.ha.common.RedisStorage
import org.ostelco.diameter.ha.logger
import org.ostelco.diameter.ha.server.ServerCCASessionDataRedisReplicatedImpl
import org.ostelco.diameter.ha.sessiondatasource.RedisReplicatedSessionDatasource

class CCARedisReplicatedSessionDataFactory

(replicatedSessionDataSource: ISessionDatasource) : IAppSessionDataFactory<ICCASessionData> {

    private val logger by logger()

    private val replicatedSessionDataSource: RedisReplicatedSessionDatasource
    private val redisStorage: RedisStorage

    init {
        this.replicatedSessionDataSource = replicatedSessionDataSource as RedisReplicatedSessionDatasource
        this.redisStorage = replicatedSessionDataSource.getRedisStorage()
    }

    override fun getAppSessionData(clazz: Class<out AppSession>, sessionId: String): ICCASessionData {

        if (clazz == ClientCCASession::class.java) {
            logger.info("Getting session of type ClientCCASession")
            val data = ClientCCASessionDataRedisReplicatedImpl(sessionId, this.redisStorage, this.replicatedSessionDataSource.container)
            return data
        } else if (clazz == ServerCCASession::class.java) {
            logger.info("Getting session of type ServerCCASession")
            val data = ServerCCASessionDataRedisReplicatedImpl(sessionId, this.redisStorage, this.replicatedSessionDataSource.container)
            return data
        }
        throw IllegalArgumentException(clazz.toString())
    }
}