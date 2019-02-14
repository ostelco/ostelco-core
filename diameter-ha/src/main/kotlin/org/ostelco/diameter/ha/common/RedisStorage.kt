package org.ostelco.diameter.ha.common

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import java.util.concurrent.TimeUnit


class RedisStorage : ReplicatedStorage {

    private val redisURI = RedisURI.Builder.redis(getRedisHostName(), getRedisPort()).build()
    private val redisClient : RedisClient = RedisClient.create(redisURI)
    private lateinit var connection : StatefulRedisConnection<String, String>
    private lateinit var asyncCommands: RedisAsyncCommands<String, String>


    override fun start() {
        connection = redisClient.connect()
        asyncCommands = connection.async()
    }

    override fun storeValue(id: String, key: String, value: String) : Boolean {
        asyncCommands.hset(id, key, value)
        // Keys will be auto deleted from Redis if not updated within 3 days
        asyncCommands.expire(id, 259200)
        return true
    }

    override fun getValue(id:String, key: String): String? {
        return asyncCommands.hget(id,key).get(5, TimeUnit.SECONDS)
    }

    override fun removeValue(id:String, key: String) {
        asyncCommands.hdel(id, key)
    }

    override fun removeId(id: String) {
        val keys = asyncCommands.hkeys(id).get(5, TimeUnit.SECONDS)
        keys.forEach { key ->
            removeValue(id, key)
        }
    }

    override fun exist(id: String) : Boolean {
        return (asyncCommands.hlen(id).get(5, TimeUnit.SECONDS) > 0)
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