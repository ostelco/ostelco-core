package org.ostelco.diameter.ha.sessiondatasource

import org.jdiameter.api.BaseSession
import org.jdiameter.api.IllegalDiameterStateException
import org.jdiameter.api.NetworkReqListener
import org.jdiameter.client.api.IContainer
import org.jdiameter.client.api.ISessionFactory
import org.jdiameter.common.api.app.IAppSessionData
import org.jdiameter.common.api.app.IAppSessionDataFactory
import org.jdiameter.common.api.app.acc.IAccSessionData
import org.jdiameter.common.api.app.auth.IAuthSessionData
import org.jdiameter.common.api.app.cca.ICCASessionData
import org.jdiameter.common.api.app.cxdx.ICxDxSessionData
import org.jdiameter.common.api.app.gx.IGxSessionData
import org.jdiameter.common.api.app.rf.IRfSessionData
import org.jdiameter.common.api.app.ro.IRoSessionData
import org.jdiameter.common.api.app.rx.IRxSessionData
import org.jdiameter.common.api.app.s13.IS13SessionData
import org.jdiameter.common.api.app.sh.IShSessionData
import org.jdiameter.common.api.app.slg.ISLgSessionData
import org.jdiameter.common.api.app.slh.ISLhSessionData
import org.jdiameter.common.api.data.ISessionDatasource
import org.jdiameter.common.impl.app.acc.AccLocalSessionDataFactory
import org.jdiameter.common.impl.app.auth.AuthLocalSessionDataFactory
import org.jdiameter.common.impl.app.cxdx.CxDxLocalSessionDataFactory
import org.jdiameter.common.impl.app.gx.GxLocalSessionDataFactory
import org.jdiameter.common.impl.app.rf.RfLocalSessionDataFactory
import org.jdiameter.common.impl.app.ro.RoLocalSessionDataFactory
import org.jdiameter.common.impl.app.rx.RxLocalSessionDataFactory
import org.jdiameter.common.impl.app.s13.S13LocalSessionDataFactory
import org.jdiameter.common.impl.app.sh.ShLocalSessionDataFactory
import org.jdiameter.common.impl.app.slg.SLgLocalSessionDataFactory
import org.jdiameter.common.impl.app.slh.SLhLocalSessionDataFactory
import org.jdiameter.common.impl.data.LocalDataSource
import org.ostelco.diameter.ha.common.AppSessionDataReplicatedImpl
import org.ostelco.diameter.ha.common.RedisStorage
import org.ostelco.diameter.ha.logger
import org.ostelco.diameter.ha.sessiondatafactory.CCAReplicatedSessionDataFactory
import java.util.HashMap

/**
 * A Replicated DataSource that will use Redis as a remote store to save session information.
 */
class RedisReplicatedSessionDatasource(val container: IContainer) : ISessionDatasource {

    private val logger by logger()
    private val localDataSource: ISessionDatasource = LocalDataSource()

    private var appSessionDataFactories = HashMap<Class<out IAppSessionData>, IAppSessionDataFactory<out IAppSessionData>>()

    private val redisStorage = RedisStorage()


    // We only care about ICCASessionData so that is the only one we have re-implemented right now
    init {
        appSessionDataFactories[IAuthSessionData::class.java] = AuthLocalSessionDataFactory()
        appSessionDataFactories[IAccSessionData::class.java] = AccLocalSessionDataFactory()
        appSessionDataFactories[ICCASessionData::class.java] = CCAReplicatedSessionDataFactory(this, redisStorage)
        appSessionDataFactories[IRoSessionData::class.java] = RoLocalSessionDataFactory()
        appSessionDataFactories[IRfSessionData::class.java] = RfLocalSessionDataFactory()
        appSessionDataFactories[IShSessionData::class.java] = ShLocalSessionDataFactory()
        appSessionDataFactories[ICxDxSessionData::class.java] = CxDxLocalSessionDataFactory()
        appSessionDataFactories[IGxSessionData::class.java] = GxLocalSessionDataFactory()
        appSessionDataFactories[IRxSessionData::class.java] = RxLocalSessionDataFactory()
        appSessionDataFactories[IS13SessionData::class.java] = S13LocalSessionDataFactory()
        appSessionDataFactories[ISLhSessionData::class.java] = SLhLocalSessionDataFactory()
        appSessionDataFactories[ISLgSessionData::class.java] = SLgLocalSessionDataFactory()
    }

    override fun isClustered(): Boolean {
        return false
    }

    override fun start() {
        logger.debug("start")
        redisStorage.start()
    }

    override fun stop() {
        logger.debug("stop")
        redisStorage.stop()
    }

    override fun setSessionListener(sessionId: String?, data: NetworkReqListener?) {
        if (localDataSource.exists(sessionId)) {
            localDataSource.setSessionListener(sessionId, data)
        } else {
            logger.error("Could not set session listener for non local session $sessionId")
        }
    }

    override fun removeSessionListener(sessionId: String?): NetworkReqListener? {
        if (localDataSource.exists(sessionId)) {
            return localDataSource.removeSessionListener(sessionId)
        } else {
            logger.error("Could not remove SessionListener for session $sessionId")
        }
        return null
    }

    override fun removeSession(sessionId: String?) {
        if (sessionId != null) {
            if (localDataSource.exists(sessionId)) {
                localDataSource.removeSession(sessionId)
            } else if (existReplicated(sessionId)) {
                redisStorage.removeId(sessionId)
            } else {
                logger.error("Could not remove session : $sessionId. Not found")
            }
        }
    }

    override fun getSession(sessionId: String?): BaseSession? {
        if (sessionId != null) {
            when {
                localDataSource.exists(sessionId) -> {
                    logger.debug("Using LocalDataSource for session $sessionId")
                    return localDataSource.getSession(sessionId)
                }
                existReplicated(sessionId) -> {
                    logger.debug("Using replicated session : $sessionId")
                    makeLocal(sessionId)
                    return localDataSource.getSession(sessionId)
                }
                else -> logger.error("Session not local or external $sessionId")
            }
        }
        return null
    }

    override fun exists(sessionId: String?): Boolean {
        return if (localDataSource.exists(sessionId)) true else existReplicated(sessionId)
    }

    override fun getSessionListener(sessionId: String?): NetworkReqListener? {
        when {
            localDataSource.exists(sessionId) -> {
                logger.debug("Getting session listener from localDataSource for sessionId $sessionId")
                return localDataSource.getSessionListener(sessionId)
            }
            existReplicated(sessionId) -> {
                logger.debug("Getting session listener from replicated external source for sessionId $sessionId")
                makeLocal(sessionId)
                return localDataSource.getSessionListener(sessionId)
            }
            else -> logger.debug("Could not find existing session listener for sessionId $sessionId")
        }
        return null
    }

    override fun getDataFactory(x: Class<out IAppSessionData>?): IAppSessionDataFactory<out IAppSessionData>? {
        return appSessionDataFactories[x]
    }

    override fun addSession(session: BaseSession?) {
        localDataSource.addSession(session)
    }

    private fun makeLocal(sessionId: String?) {
        logger.info("MakeLocal sessionId $sessionId")
        if (sessionId != null) {
            try {
                val appSessionInterfaceClass = AppSessionDataReplicatedImpl.getAppSessionIface(redisStorage, sessionId)
                val factory = (container.sessionFactory as ISessionFactory).getAppSessionFactory(appSessionInterfaceClass)
                if (factory == null) {
                    logger.warn("Session with id: $sessionId, is in replicated data source, but no Application Session Factory for: $appSessionInterfaceClass.")
                    return
                } else {
                    val session = factory.getSession(sessionId, appSessionInterfaceClass)
                    localDataSource.addSession(session)
                    localDataSource.setSessionListener(sessionId, session as NetworkReqListener)
                    return
                }
            } catch (e: IllegalDiameterStateException) {
                logger.error("Failed to obtain factory from stack.", e)
            }
        } else {
            logger.error("No sessionId set")
        }
    }

    private fun existReplicated(sessionId: String?): Boolean {
        var sessionIdExist = false
        if (sessionId != null) {
            sessionIdExist = redisStorage.exist(sessionId)
        }

        logger.debug("existReplicated {} {}", sessionId, sessionIdExist)

        return sessionIdExist
    }
}