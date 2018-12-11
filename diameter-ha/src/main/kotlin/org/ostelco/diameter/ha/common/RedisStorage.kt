package org.ostelco.diameter.ha.common

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import java.io.Serializable

class RedisStorage {

    private val redisClient : RedisClient
    private lateinit var connection : StatefulRedisConnection<String, String>
    private lateinit var commands : RedisCommands<String, String>

    init {
        redisClient = RedisClient.create("redis://127.0.0.1:6379")
    }


    fun connect() {
        connection = redisClient.connect()
        commands = connection.sync()
    }

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
    fun storeValue(id:String, key: String, value: String) : Boolean {
        return commands.hset(id, key, value)
    }

    /**
     * Get a key value pair for an id
     *
     * @param id the sessionId for the value to retrieve
     * @param key
     * @return String with the value associated for the key and id, or null if not present
     */
    fun getValue(id:String, key: String): String? {
        return commands.hget(id, key)
    }

    /**
     * Remove a key value pair for an id
     *
     * @param id the sessionId for the value to remove
     * @param key
     */
    fun removeValue(id:String, key: String) {
        commands.hdel(id, key)
    }

    /**
     * Remove all key value pairs for an id
     *
     * @param id the sessionId for the value to remove
     */
    fun removeId(id: String) {
        val keys = commands.hkeys(id);
        keys.forEach { key ->
            removeValue(id, key)
        }
    }

    /**
     * Check if session id has been stored
     *
     * @param id the sessionId
     */
    fun exist(id: String) : Boolean {
        return (commands.hlen(id) > 0)
    }

    fun disconnect() {
        connection.close()
        redisClient.shutdown()
    }
}