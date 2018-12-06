package org.ostelco.diameter.ha.common

import org.jdiameter.api.ApplicationId
import org.jdiameter.common.api.app.IAppSessionData

open class AppSessionDataRedisReplicatedImpl : IAppSessionData {
    /**
     * Sets the Application-Id of this Session Data session to which this data belongs to.
     * @param applicationId the Application-Id
     */
    override fun setApplicationId(applicationId: ApplicationId?) {
        TODO("not implemented")
    }

    /**
     * Returns the Application-Id of this Session Data session to which this data belongs to.
     *
     * @return the Application-Id
     */
    override fun getApplicationId(): ApplicationId {
        TODO("not implemented")
    }

    /**
     * Removes this session data from storage
     *
     * @return true if removed, false otherwise
     */
    override fun remove(): Boolean {
        TODO("not implemented")
    }

    /**
     * Returns the session-id of the session to which this data belongs to.
     * @return a string representing the session-id
     */
    override fun getSessionId(): String {
        TODO("not implemented")
    }
}