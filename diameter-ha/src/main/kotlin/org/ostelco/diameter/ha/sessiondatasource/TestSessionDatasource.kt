package org.ostelco.diameter.ha.sessiondatasource

import org.jdiameter.api.BaseSession
import org.jdiameter.api.NetworkReqListener
import org.jdiameter.common.api.app.IAppSessionData
import org.jdiameter.common.api.app.IAppSessionDataFactory
import org.jdiameter.common.api.data.ISessionDatasource
import org.ostelco.diameter.ha.logger

public class TestSessionDatasource : ISessionDatasource {

    private val logger by logger()

    override fun isClustered(): Boolean {
        logger.info("isClustered")
        return true;
    }

    override fun start() {
        logger.info("start")
    }

    override fun stop() {
        logger.info("stop")
    }

    override fun setSessionListener(sessionId: String?, data: NetworkReqListener?) {
        logger.info("setSessionListener sessionId: $sessionId data: $data")
    }

    override fun removeSessionListener(sessionId: String?): NetworkReqListener {
        TODO("Implement this")
        logger.info("removeSessionListener sessionId: $sessionId")
    }

    override fun removeSession(sessionId: String?) {
        logger.info("removeSession sessionId:$sessionId")
    }

    override fun getSession(sessionId: String?): BaseSession {
        TODO("Implement this")
        logger.info("getSession $sessionId")
    }

    override fun exists(sessionId: String?): Boolean {
        logger.info("exists sessionId: $sessionId")
        return true;
    }

    override fun getSessionListener(sessionId: String?): NetworkReqListener {
        TODO("Implement this")
        logger.info("getSessionListener sessionId:$sessionId")
    }

    override fun getDataFactory(x: Class<out IAppSessionData>?): IAppSessionDataFactory<out IAppSessionData> {
        TODO("Implement this")
        logger.info("getDataFactory x:$x")
    }

    override fun addSession(session: BaseSession?) {
        logger.info("addSession session:$session")
    }

}