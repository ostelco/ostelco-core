package org.ostelco.diameter.ha.common

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import java.lang.Error

class RedisStorage : ReplicatedStorage {

    // ToDo : Redis configuration from env
    // ToDo : Default timeout for Redis to delete session
    private val redisURI = RedisURI.Builder.redis(getRedisHostName(), getRedisPort()).build();
    private val redisClient : RedisClient = RedisClient.create(redisURI)
    private lateinit var connection : StatefulRedisConnection<String, String>
    private lateinit var commands : RedisCommands<String, String>


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

    private fun getRedisHostName() : String {
        var hostname = System.getenv("REDIS_HOSTNAME")
        if (hostname == null || hostname.isEmpty()) {
            hostname = "localhost"
        }
        return hostname
    }

    private fun getRedisPort() : Int {
        val portEnv = System.getenv("REDIS_PORT")
        var port = 6379
        if (portEnv != null) {
            port = portEnv.toInt()
        }
        return port
    }
}