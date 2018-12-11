package org.ostelco.diameter.ha.common

import org.jdiameter.api.ApplicationId
import org.jdiameter.common.api.app.IAppSessionData
import org.ostelco.diameter.ha.logger
import java.io.*
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.util.*

open class AppSessionDataRedisReplicatedImpl(val id: String, val redisStorage: RedisStorage) : IAppSessionData {

    protected val APID = "APID"
    protected val SIFACE = "SIFACE"

    private val logger by logger()

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
        storeValue(APID, toBase64String(applicationId))
    }

    /**
     * Returns the Application-Id of this Session Data session to which this data belongs to.
     *
     * @return the Application-Id
     */
    override fun getApplicationId(): ApplicationId {
        val value = redisStorage.getValue(id, APID)
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
        if (redisStorage.exist(id)) {
            redisStorage.removeId(id)
            return true
        } else {
            return false
        }
    }

    protected fun toPrimitive(boolString: String?, default: Boolean): Boolean {
        if (boolString != null) {
            return boolString.toBoolean()
        }
        return default
    }

    protected fun storeValue(key: String, value: String) : Boolean {
        logger.info("Storing key : $key value : $value")
        return  this.redisStorage.storeValue(id, key, value)
    }

    /**
     * Convert ByteBuffer to a b64 encoded string
     */
    @Throws(IOException::class)
    protected fun ByteBuffertoBase64String(data: ByteBuffer): String {
        val array = ByteArray(data.remaining())
        data.get(array)
        return Base64.getEncoder().encodeToString(array)
    }

    /**
     * Read the object from Base64 string.
     **/
    @Throws(IOException::class, ClassNotFoundException::class)
    protected fun ByteArrayfromBase64String(b64String: String): ByteArray? {
        return Base64.getDecoder().decode(b64String)
    }

    /**
     * Convert Serializable to a b64 encoded string
     */
    @Throws(IOException::class)
    protected fun toBase64String(o: Serializable?): String {
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(o)
        oos.close()
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    /**
     * Read the object from Base64 string.
     **/
    @Throws(IOException::class, ClassNotFoundException::class)
    protected fun fromBase64String(b64String: String): Serializable {
        val data = Base64.getDecoder().decode(b64String)
        val objectInputStream = ObjectInputStream(
                ByteArrayInputStream(data))
        val any = objectInputStream.readObject() as Serializable
        objectInputStream.close()
        return any
    }
}