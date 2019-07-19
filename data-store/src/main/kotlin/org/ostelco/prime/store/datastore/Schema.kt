package org.ostelco.prime.store.datastore

import arrow.core.Either
import arrow.core.Try
import com.fasterxml.jackson.core.type.TypeReference
import com.google.cloud.NoCredentials
import com.google.cloud.Timestamp
import com.google.cloud.datastore.Blob
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.EntityQuery
import com.google.cloud.datastore.FullEntity
import com.google.cloud.datastore.IncompleteKey
import com.google.cloud.datastore.Key
import com.google.cloud.datastore.LatLng
import com.google.cloud.datastore.NullValue
import com.google.cloud.datastore.PathElement
import com.google.cloud.datastore.Query
import com.google.cloud.datastore.QueryResults
import com.google.cloud.datastore.StringValue
import com.google.cloud.datastore.StructuredQuery.PropertyFilter
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.google.cloud.http.HttpTransportOptions
import org.ostelco.prime.jsonmapper.objectMapper


class EntityStore<E>(
        private val entityClass: Class<E>,
        type: String = "inmemory-emulator",
        namespace: String = "") {

    private val datastore: Datastore

    private var localDatastoreHelper: LocalDatastoreHelper? = null

    init {
        datastore = when (type) {
            "inmemory-emulator" -> {
                localDatastoreHelper = LocalDatastoreHelper.create(1.0)
                localDatastoreHelper?.start()
                localDatastoreHelper?.options ?: throw Exception("Failed to start inmemory-emulator")
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
    }

    fun fetchAll(parentKind: String, parentKeyString: String): Either<Throwable, Collection<E>> = Try {
        fetch(
                query = { entityQueryBuilder ->
                    entityQueryBuilder.setFilter(
                            PropertyFilter.hasAncestor(datastore
                                    .newKeyFactory()
                                    .setKind(parentKind)
                                    .newKey(parentKeyString)
                            )
                    )
                }
        )
    }.toEither()

    fun fetch(keyString: String, vararg parents: Pair<String, String>): Either<Throwable, E> = Try {
        val keyFactory = parents.fold(initial = datastore.newKeyFactory().setKind(entityClass.name)) { factory, parent ->
            factory.addAncestor(PathElement.of(parent.first, parent.second))
        }
        entityToObject(datastore.get(keyFactory.newKey(keyString)))
    }.toEither()

    fun fetch(key: Key): Either<Throwable, E> = Try {
        entityToObject(datastore.get(key))
    }.toEither()

    private fun entityToObject(entity: Entity): E {
        val map = entity
                .names
                .map { name ->
                    val value = when (entityClass.getDeclaredField(name).type) {
                        Key::class.java -> entity.getKey(name)
                        Long::class.java -> entity.getLong(name)
                        Blob::class.java -> entity.getBlob(name)
                        Double::class.java -> entity.getDouble(name)
                        Boolean::class.java -> entity.getBoolean(name)
                        LatLng::class.java -> entity.getLatLng(name)
                        String::class.java -> entity.getString(name)
                        StringValue::class.java -> entity.getValue<StringValue>(name).get()
                        Timestamp::class.java -> entity.getTimestamp(name)
                        else -> null
                    }
                    Pair(name, value)
                }
                .toMap()
        return objectMapper.convertValue(map, entityClass)
    }

    fun add(
            entity: E,
            keyString: String? = null,
            vararg parents: Pair<String, String>) = save(datastore::add, entity, keyString, *parents)

    fun put(
            entity: E,
            keyString: String? = null,
            vararg parents: Pair<String, String>) = save(datastore::put, entity, keyString, *parents)

    private fun save(
            operation: (FullEntity<IncompleteKey>) -> Entity,
            entity: E,
            keyString: String? = null,
            vararg parents: Pair<String, String>): Either<Throwable, Key> = Try {

        // convert object to map of (field name, field value)
        // TODO: Fails to serialize datastore 'Value<*>' types such as 'StringValue'.
        val map: Map<String, Any?> = objectMapper.convertValue(entity, object : TypeReference<Map<String, Any?>>() {})

        val keyFactory = parents.fold(initial = datastore.newKeyFactory().setKind(entityClass.name)) { factory, parent ->
            factory.addAncestor(PathElement.of(parent.first, parent.second))
        }

        // Entity Builder
        val entityBuilder = FullEntity.newBuilder(keyString?.let { keyFactory.newKey(keyString) } ?: keyFactory.newKey())

        val fieldsToExclude = fieldsToExcludeFromIndex(entity)
        val excludeFromIndex: (String) -> Boolean = { x -> fieldsToExclude[x] == true }

        // for each field, call appropriate setter
        // TODO: Add support for 'datastore-exclude-from-index' annotation for other types
        map.forEach { (key, value) ->
            when (value) {
                null -> entityBuilder.set(key, NullValue.of())
                is Key -> entityBuilder.set(key, value)
                is Long -> entityBuilder.set(key, value)
                is Blob -> entityBuilder.set(key, value)
                is Double -> entityBuilder.set(key, value)
                is Boolean -> entityBuilder.set(key, value)
                is LatLng -> entityBuilder.set(key, value)
                is String -> {
                    if (excludeFromIndex(key))
                        entityBuilder.set(key, StringValue.newBuilder(value)
                                .setExcludeFromIndexes(true)
                                .build())
                    else
                        entityBuilder.set(key, value)
                }
                is Timestamp -> entityBuilder.set(key, value)
            }
        }

        entityBuilder.build().let(operation).key
    }.toEither()

    private fun fieldsToExcludeFromIndex(target: Any?): Map<String, Boolean> {
        if (target == null) return mapOf()

        val map = mutableMapOf<String, Boolean>()
        val declaredFields = target::class.java.declaredFields

        declaredFields.forEach { field ->
            field.annotations.forEach {
                when (it) {
                    is DatastoreExcludeFromIndex -> map[field.name] = true
                    else -> map[field.name] = false
                }
            }
        }

        return map.toMap()
    }

    fun fetch(query: (EntityQuery.Builder) -> EntityQuery.Builder,
              onAfterQueryResultRead: (QueryResults<Entity>) -> Unit = {}): Collection<E> {
        val queryBuilder = Query.newEntityQueryBuilder().setKind(entityClass.name)
        val queryResults = datastore.run(query(queryBuilder).build())
        val returnList = queryResults.asSequence().map(this::entityToObject).toList()
        onAfterQueryResultRead(queryResults)
        return returnList
    }

    fun delete(keyString: String, vararg parents: Pair<String, String>) = Try {
        val keyFactory = parents.fold(initial = datastore.newKeyFactory().setKind(entityClass.name)) { factory, parent ->
            factory.addAncestor(PathElement.of(parent.first, parent.second))
        }
        datastore.delete(keyFactory.newKey(keyString))
    }.toEither()

    fun close() {
        localDatastoreHelper?.stop()
    }
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class DatastoreExcludeFromIndex
