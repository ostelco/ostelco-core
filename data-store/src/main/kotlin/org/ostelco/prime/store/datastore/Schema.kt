package org.ostelco.prime.store.datastore

import arrow.core.Either
import arrow.core.Try
import arrow.core.left
import arrow.core.right
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

    fun fetch(key: Key): Either<Throwable, T> = Try {
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
                        StringValue::class.java -> fullEntity.getValue<StringValue>(name).get()
                        Timestamp::class.java -> fullEntity.getTimestamp(name)
                        else -> null
                    }
                    Pair(name, value)
                }
                .toMap()
        objectMapper.convertValue(map, entityClass)
    }.toEither()

    fun add(target: T): Either<Throwable, Key> = Try {
        // convert object to map of (field name, field value)
        // TODO: Fails to serialize datastore 'Value<*>' types such as 'StringValue'.
        val map: Map<String, Any?> = objectMapper.convertValue(target, object : TypeReference<Map<String, Any?>>() {})

        // Entity Builder
        val entity = FullEntity.newBuilder(keyFactory.newKey())

        val fieldsToExclude = fieldsToExcludeFromIndex(target)
        val excludeFromIndex: (String) -> Boolean = { x -> fieldsToExclude.get(x) == true }

        // for each field, call appropriate setter
        // TODO: Add support for 'datastore-exclude-from-index' annotation for other types
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
                    if (excludeFromIndex(key))
                        entity.set(key, StringValue.newBuilder(value)
                                .setExcludeFromIndexes(true)
                                .build())
                    else
                        entity.set(key, value)
                }
                is Timestamp -> entity.set(key, value)
            }
        }

        datastore.add(entity.build()).key
    }.toEither()

    private fun fieldsToExcludeFromIndex(target: Any?): Map<String, Boolean> {
        if (target == null) return mapOf()

        val map = mutableMapOf<String, Boolean>()
        val declaredFields = target::class.java.declaredFields

        declaredFields.forEach { field ->
            field.annotations.forEach {
                when (it) {
                    is DatastoreExcludeFromIndex -> map.put(field.name, true)
                    else -> map.put(field.name, false)
                }
            }
        }

        return map.toMap()
    }
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class DatastoreExcludeFromIndex
