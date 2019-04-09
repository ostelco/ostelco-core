package org.ostelco.prime.store.datastore

import com.fasterxml.jackson.core.type.TypeReference
import com.google.cloud.NoCredentials
import com.google.cloud.Timestamp
import com.google.cloud.datastore.*
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.google.cloud.http.HttpTransportOptions
import org.ostelco.prime.jsonmapper.objectMapper


class EntityStore<T>(
        private val entityClass: Class<T>,
        type: String = "inmemory-emulator",
        namespace: String = "") {

    private val keyFactory: KeyFactory
    private val datastore: Datastore

    init {
        datastore = when (type) {
            "inmemory-emulator" -> {
                val localDatastoreHelper = LocalDatastoreHelper.create(1.0)
                localDatastoreHelper.start()
                localDatastoreHelper.options
            }
            "emulator" -> {
                // When prime running in GCP by hosted CI/CD, Datastore client library assumes it is running in
                // production and ignore our instruction to connect to the datastore emulator. So, we are explicitly
                // connecting to emulator
                // logger.info("Connecting to datastore emulator")
                DatastoreOptions
                        .newBuilder()
                        .setHost("localhost:9090")
                        .setCredentials(NoCredentials.getInstance())
                        .setTransportOptions(HttpTransportOptions.newBuilder().build())
                        .build()
            }
            else -> {
                // logger.info("Created default instance of datastore client")
                DatastoreOptions
                        .newBuilder()
                        .setNamespace(namespace)
                        .build()
            }
        }.service
        keyFactory = datastore.newKeyFactory().setKind(entityClass.name)
    }

    fun fetch(key: Key): T {
        val fullEntity = datastore.fetch(key).single()
        val map = fullEntity
                .names
                .map { name ->
                    val value = when (entityClass.getDeclaredField(name).type) {
                        Key::class.java -> fullEntity.getKey(name)
                        Long::class.java -> fullEntity.getLong(name)
                        Blob::class.java -> fullEntity.getBlob(name)
                        Double::class.java -> fullEntity.getDouble(name)
                        Boolean::class.java -> fullEntity.getBoolean(name)
                        LatLng::class.java -> fullEntity.getLatLng(name)
                        String::class.java -> fullEntity.getString(name)
                        StringValue::class.java -> (fullEntity.getValue(name) as StringValue).get()
                        Timestamp::class.java -> fullEntity.getTimestamp(name)
                        else -> null
                    }
                    Pair(name, value)
                }
                .toMap()
        return objectMapper.convertValue(map, entityClass)
    }

    fun add(t: T): Key {
        // convert object to map of (field name, field value)
        // TODO: Fails to serialize datastore 'Value<*>' types such as 'StringValue'.
        val map: Map<String, Any?> = objectMapper.convertValue(t, object : TypeReference<Map<String, Any?>>() {})

        // Entity Builder
        val entity = FullEntity.newBuilder(keyFactory.newKey())

        // for each field, call appropriate setter
        map.forEach { key, value ->
            when (value) {
                null -> entity.set(key, NullValue.of())
                is Key -> entity.set(key, value)
                is Long -> entity.set(key, value)
                is Blob -> entity.set(key, value)
                is Double -> entity.set(key, value)
                is Boolean -> entity.set(key, value)
                is LatLng -> entity.set(key, value)
                is String -> {
                    // Workaround for handling long strings, and the inability
                    // of 'objectMapper' to serialize 'StringValue' objects.
                    if (value.toByteArray().size > MAX_STRING_SIZE)
                        entity.set(key, StringValue.newBuilder(value)
                                .setExcludeFromIndexes(true)
                                .build())
                    else
                        entity.set(key, value)
                }
                is Timestamp -> entity.set(key, value)
            }
        }
        return datastore.add(entity.build()).key
    }

    companion object {
        // Max size of strings to be stored to Datastore with index.
        val MAX_STRING_SIZE = 1500
    }
}