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
import org.ostelco.diameter.ha.common.ReplicatedStorage
import org.ostelco.diameter.ha.logger
import org.ostelco.diameter.ha.sessiondatafactory.CCAReplicatedSessionDataFactory
import java.util.HashMap

/**
 * A Replicated DataSource that will use redis as a remote store to save session information.
 */
class RedisReplicatedSessionDatasource(val container: IContainer) : ISessionDatasource {

    private val logger by logger()
    private val localDataSource: ISessionDatasource = LocalDataSource()

    protected var appSessionDataFactories = HashMap<Class<out IAppSessionData>, IAppSessionDataFactory<out IAppSessionData>>()

    private val redisStorage = RedisStorage()


    // We only care about ICCASessionData so that is the only one we have re-implemented right now
    init {
        appSessionDataFactories[IAuthSessionData::class.java] = AuthLocalSessionDataFactory()
        appSessionDataFactories[IAccSessionData::class.java] = AccLocalSessionDataFactory()
        appSessionDataFactories[ICCASessionData::class.java] = CCAReplicatedSessionDataFactory(this)
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
        logger.info("isClustered")
        return false;
    }

    override fun start() {
        logger.info("start")
        redisStorage.start()
    }

    override fun stop() {
        logger.info("stop")
        redisStorage.stop()
    }

    override fun setSessionListener(sessionId: String?, data: NetworkReqListener?) {
        logger.info("setSessionListener sessionId: $sessionId data: $data")
        if (localDataSource.exists(sessionId)) {
            localDataSource.setSessionListener(sessionId, data)
        } else {
            logger.error("could not set session listener for non local session $sessionId")
        }
    }

    override fun removeSessionListener(sessionId: String?): NetworkReqListener? {
        logger.info("removeSessionListener sessionId: $sessionId")
        if (localDataSource.exists(sessionId)) {
            return localDataSource.removeSessionListener(sessionId)
        } else {
            logger.error("could not remove SessionListener for session $sessionId")
        }
        return null
    }

    override fun removeSession(sessionId: String?) {
        if (sessionId != null) {
            logger.info("removeSession sessionId:$sessionId")
            if (localDataSource.exists(sessionId)) {
                localDataSource.removeSession(sessionId)
            } else if (existReplicated(sessionId)) {
                logger.info("Removed external session information")
                redisStorage.removeId(sessionId)
            } else {
                logger.error("Could not remove session : $sessionId. Not found")
            }
        }
    }

    override fun getSession(sessionId: String?): BaseSession? {
        logger.info("getSession $sessionId")
        if (sessionId != null) {
            if (this.localDataSource.exists(sessionId)) {
                logger.info("Using LocalDataSouce for session $sessionId")
                return this.localDataSource.getSession(sessionId)
            } else if (existReplicated(sessionId)) {
                logger.info("Using replicated session : $sessionId")
                makeLocal(sessionId)
                return this.localDataSource.getSession(sessionId)
            } else {
                logger.error("Session not local or external $sessionId")
            }
        }
        return null
    }

    override fun exists(sessionId: String?): Boolean {
        logger.info("exists sessionId: $sessionId")
        return if (this.localDataSource.exists(sessionId)) true else this.existReplicated(sessionId)
    }

    override fun getSessionListener(sessionId: String?): NetworkReqListener? {
        logger.info("getSessionListener sessionId:$sessionId")
        if (localDataSource.exists(sessionId)) {
            return localDataSource.getSessionListener(sessionId)
        } else if (existReplicated(sessionId)) {
            logger.info("getting session listener from replicatad external source")
            makeLocal(sessionId)
            return localDataSource.getSessionListener(sessionId)
        } else {
            logger.error("Could not get session listener for sessionId $sessionId")
        }
        return null
    }

    override fun getDataFactory(x: Class<out IAppSessionData>?): IAppSessionDataFactory<out IAppSessionData>? {
        logger.info("getDataFactory x:$x")
        return this.appSessionDataFactories[x]
    }

    override fun addSession(session: BaseSession?) {
        logger.info("addSession session: $session")
        this.localDataSource.addSession(session)
    }

    private fun makeLocal(sessionId: String?) {
        logger.info("makeLocal")
        if (sessionId != null) {
            try {
                // this is APP session, always
                val appSessionInterfaceClass = AppSessionDataReplicatedImpl.getAppSessionIface(this.redisStorage, sessionId)
                logger.info("Got appSessionInterfaceClass : $appSessionInterfaceClass")
                // get factory;
                val factory = (this.container.sessionFactory as ISessionFactory).getAppSessionFactory(appSessionInterfaceClass)
                if (factory == null) {
                    logger.warn("Session with id:{}, is in replicated data source, but no Application Session Factory for:{}.", sessionId, appSessionInterfaceClass)
                    return
                } else {
                    logger.info("Got a factory : $factory")
                    val session = factory.getSession(sessionId, appSessionInterfaceClass)
                    this.localDataSource.addSession(session)
                    this.localDataSource.setSessionListener(sessionId, session as NetworkReqListener)
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
        if (sessionId == null) {
            return false
        } else {
            return redisStorage.exist(sessionId)
        }
    }

    fun getReplicatedStorage(): ReplicatedStorage {
        return redisStorage
    }
}