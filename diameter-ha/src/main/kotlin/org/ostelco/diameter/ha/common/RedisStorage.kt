package org.ostelco.diameter.ha.common

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands

class RedisStorage : ReplicatedStorage {

    private val redisClient : RedisClient
    private lateinit var connection : StatefulRedisConnection<String, String>
    private lateinit var commands : RedisCommands<String, String>

    init {
        redisClient = RedisClient.create("redis://127.0.0.1:6379")
    }


    override fun start() {
        connection = redisClient.connect()
        commands = connection.sync()
    }

    override fun storeValue(id: String, key: String, value: String) : Boolean {
        return commands.hset(id, key, value)
    }

    override fun getValue(id:String, key: String): String? {
        return commands.hget(id, key)
    }

    override fun removeValue(id:String, key: String) {
        commands.hdel(id, key)
    }

    override fun removeId(id: String) {
        val keys = commands.hkeys(id);
        keys.forEach { key ->
            removeValue(id, key)
        }
    }

    override fun exist(id: String) : Boolean {
        return (commands.hlen(id) > 0)
    }

    override fun stop() {
        connection.close()
        redisClient.shutdown()
    }
}