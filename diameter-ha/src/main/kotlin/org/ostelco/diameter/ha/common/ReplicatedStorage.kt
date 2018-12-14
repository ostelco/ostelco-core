package org.ostelco.diameter.ha.common

interface ReplicatedStorage {

    /**
     * Initialize the storage
     */
    fun start()

    /**
     * Shutdown storage
     */
    fun stop()


    /**
     * Store a key value pair in
     *
     * @param id the sessionId for the value to will store
     * @param key
     * @param value
     * @return Boolean integer-reply specifically:
     *
     *         {@literal true} if {@code key} is a new key and {@code value} was set. {@literal false} if
     *         {@code key} already exists for the {@code id} and the value was updated.
     */
    fun storeValue(id: String, key: String, value: String) : Boolean


    /**
     * Get a key value pair for an id
     *
     * @param id the sessionId for the value to retrieve
     * @param key
     * @return String with the value associated for the key and id, or null if not present
     */
    fun getValue(id:String, key: String): String?


    /**
     * Remove a key value pair for an id
     *
     * @param id the sessionId for the value to remove
     * @param key
     */
    fun removeValue(id:String, key: String)


    /**
     * Remove all key value pairs for an id
     *
     * @param id the sessionId for the value to remove
     */
    fun removeId(id: String)


    /**
     * Check if session id has been stored
     *
     * @param id the sessionId
     */
    fun exist(id: String) : Boolean
}