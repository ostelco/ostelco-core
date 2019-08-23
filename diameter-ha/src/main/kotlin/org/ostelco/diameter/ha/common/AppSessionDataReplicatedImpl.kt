package org.ostelco.diameter.ha.common

import org.jdiameter.api.ApplicationId
import org.jdiameter.api.acc.ClientAccSession
import org.jdiameter.api.app.AppSession
import org.jdiameter.common.api.app.IAppSessionData
import org.ostelco.diameter.ha.logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.ByteBuffer
import java.util.*



open class AppSessionDataReplicatedImpl(val id: String, val replicatedStorage: ReplicatedStorage) : IAppSessionData {

    private val logger by logger()

    private val apiId = "apiId"

    fun setAppSessionIface(iface: Class<out AppSession>) {
        storeValue(SIFACE, toBase64String(iface))
    }

    /**
     * Returns the session-id of the session to which this data belongs to.
     * @return a string representing the session-id
     */
    override fun getSessionId(): String {
        return id
    }

    /**
     * Sets the Application-Id of this Session Data session to which this data belongs to.
     * @param applicationId the Application-Id
     */
    override fun setApplicationId(applicationId: ApplicationId?) {
        if (applicationId != null) {
            storeValue(apiId, toBase64String(applicationId))
        }
    }

    /**
     * Returns the Application-Id of this Session Data session to which this data belongs to.
     *
     * @return the Application-Id
     */
    override fun getApplicationId(): ApplicationId {
        val value = getValue(apiId)
        if (value != null) {
            return fromBase64String(value) as ApplicationId
        } else {
            throw IllegalStateException()
        }
    }

    /**
     * Removes this session data from storage
     *
     * @return true if removed, false otherwise
     */
    override fun remove(): Boolean {
        var removed = false
        if (replicatedStorage.exist(id)) {
            logger.debug("Removing id : $id")
            replicatedStorage.removeId(id)
            removed = true
        }
        return removed
    }

    protected fun toPrimitive(boolString: String?, default: Boolean): Boolean {
        if (boolString != null) {
            return boolString.toBoolean()
        }
        return default
    }

    protected fun storeValue(key: String, value: String) : Boolean {
        val stored = this.replicatedStorage.storeValue(id, key, value)
        logger.debug("Storing key : $key , value : $value , id : $id stored : $stored" )
        return stored
    }

    protected fun getValue(key: String) : String? {
        logger.debug("Get key : $key , id : $id")
        val value = this.replicatedStorage.getValue(id, key)
        logger.debug("Got key : $key , value : $value , id : $id")
        return value
    }

    /**
     * Convert ByteBuffer to a Base64 encoded string
     */
    @Throws(IOException::class)
    protected fun byteBufferToBase64String(data: ByteBuffer): String {
        val array = ByteArray(data.remaining())
        data.get(array)
        return Base64.getEncoder().encodeToString(array)
    }

    /**
     * Read the object from Base64 string.
     **/
    @Throws(IOException::class, ClassNotFoundException::class)
    protected fun byteArrayFromBase64String(b64String: String): ByteArray? {
        return Base64.getDecoder().decode(b64String)
    }

    companion object AppSessionHelper {

        private val SIFACE = "SIFACE"

        fun getAppSessionIface(storage: ReplicatedStorage, sessionId: String): Class<out AppSession> {
            val value = storage.getValue(sessionId, SIFACE)
            if (value != null) {
                return fromBase64String(value) as Class<out AppSession>
            }
            return ClientAccSession::class.java
        }

        /**
         * Convert Serializable to a Base64 encoded string
         */
        @Throws(IOException::class)
        fun toBase64String(serializable: Serializable?): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
            objectOutputStream.writeObject(serializable)
            objectOutputStream.close()
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
        }

        /**
         * Read the object from Base64 encoded string.
         **/
        @Throws(IOException::class, ClassNotFoundException::class)
        fun fromBase64String(b64String: String): Serializable {
            val data = Base64.getDecoder().decode(b64String)
            val objectInputStream = ObjectInputStream(ByteArrayInputStream(data))
            val any = objectInputStream.readObject() as Serializable
            objectInputStream.close()
            return any
        }

    }
}