package org.ostelco.prime.storage.graph

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.core.type.TypeReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.neo4j.driver.v1.StatementResult
import org.neo4j.driver.v1.StatementResultCursor
import org.neo4j.driver.v1.Transaction
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.model.HasId
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AlreadyExistsError
import org.ostelco.prime.storage.NotCreatedError
import org.ostelco.prime.storage.NotDeletedError
import org.ostelco.prime.storage.NotFoundError
import org.ostelco.prime.storage.NotUpdatedError
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.graph.Graph.read
import org.ostelco.prime.storage.graph.Graph.write
import org.ostelco.prime.storage.graph.ObjectHandler.getProperties
import org.ostelco.prime.tracing.Trace
import java.util.concurrent.CompletionStage

//
// Schema classes
//

data class EntityType<ENTITY : HasId>(
        private val dataClass: Class<ENTITY>,
        val name: String = dataClass.simpleName) {

    var entityStore: EntityStore<ENTITY>? = null

    fun createEntity(map: Map<String, Any>): ENTITY = ObjectHandler.getObject(map, dataClass)
}

data class RelationType<FROM : HasId, RELATION, TO : HasId>(
        private val relation: Relation,
        val from: EntityType<FROM>,
        val to: EntityType<TO>,
        private val dataClass: Class<RELATION>) {

    init {
        RelationRegistry.register(relation, this)
    }
    var relationStore: BaseRelationStore? = null

    val name: String = relation.name

    fun createRelation(map: Map<String, Any>): RELATION {
        return ObjectHandler.getObject(map, dataClass)
    }
}

class EntityStore<E : HasId>(private val entityType: EntityType<E>) {

    init {
        entityType.entityStore = this
    }

    fun get(id: String, transaction: Transaction): Either<StoreError, E> {
        return read("""MATCH (node:${entityType.name} {id: '$id'}) RETURN node;""",
                transaction) { statementResult ->
            if (statementResult.hasNext())
                Either.right(entityType.createEntity(statementResult.single().get("node").asMap()))
            else
                Either.left(NotFoundError(type = entityType.name, id = id))
        }
    }

    fun create(entity: E, transaction: Transaction): Either<StoreError, Unit> {

        return doNotExist(id = entity.id, transaction = transaction).flatMap {

            val properties = getProperties(entity)
            val strProps: String = properties.entries.joinToString(separator = ",") { """ `${it.key}`: "${it.value}"""" }
                    .let { if (it.isNotBlank()) ",$it" else it }
            write("""CREATE (node:${entityType.name} { id:"${entity.id}"$strProps });""",
                    transaction) {
                if (it.summary().counters().nodesCreated() == 1)
                    Unit.right()
                else
                    Either.left(NotCreatedError(type = entityType.name, id = entity.id))
            }
        }
    }

    fun <TO : HasId> getRelated(
            id: String,
            relationType: RelationType<E, *, TO>,
            transaction: Transaction): Either<StoreError, List<TO>> {

        return exists(id, transaction).flatMap {

            read("""
                MATCH (:${relationType.from.name} {id: '$id'})-[:${relationType.name}]->(node:${relationType.to.name})
                RETURN node;
                """.trimIndent(),
                    transaction) { statementResult ->
                Either.right(
                        statementResult.list { record -> relationType.to.createEntity(record["node"].asMap()) })
            }
        }
    }

    fun <FROM : HasId> getRelatedFrom(
            id: String,
            relationType: RelationType<FROM, *, E>,
            transaction: Transaction): Either<StoreError, List<FROM>> {

        return exists(id, transaction).flatMap {

            read("""
                MATCH (node:${relationType.from.name})-[:${relationType.name}]->(:${relationType.to.name} {id: '$id'})
                RETURN node;
                """.trimIndent(),
                    transaction) { statementResult ->
                Either.right(
                        statementResult.list { record -> relationType.from.createEntity(record["node"].asMap()) })
            }
        }
    }

    fun <RELATION : Any> getRelations(
            id: String,
            relationType: RelationType<E, RELATION, *>,
            transaction: Transaction): Either<StoreError, List<RELATION>> {

        return exists(id, transaction).flatMap {

            read("""
                MATCH (from:${entityType.name} { id: '$id' })-[r:${relationType.name}]-()
                return r;
                """.trimIndent(),
                    transaction) { statementResult ->
                statementResult.list { record -> relationType.createRelation(record["r"].asMap()) }
                        .right()
            }
        }
    }

    fun update(entity: E, transaction: Transaction): Either<StoreError, Unit> {

        return exists(entity.id, transaction).flatMap {
            val properties = getProperties(entity)
            // TODO vihang: replace setClause with map based settings written by Kjell
            val setClause: String = properties.entries.fold("") { acc, entry -> """$acc SET node.`${entry.key}` = '${entry.value}' """ }
            write("""MATCH (node:${entityType.name} { id: '${entity.id}' }) $setClause ;""",
                    transaction) { statementResult ->
                Either.cond(
                        test = statementResult.summary().counters().containsUpdates(), // TODO vihang: this is not perfect way to check if updates are applied
                        ifTrue = {},
                        ifFalse = { NotUpdatedError(type = entityType.name, id = entity.id) })
            }
        }
    }

    fun delete(id: String, transaction: Transaction): Either<StoreError, Unit> =
            exists(id, transaction).flatMap {
                write("""MATCH (node:${entityType.name} {id: '$id'} ) DETACH DELETE node;""",
                        transaction) { statementResult ->
                    Either.cond(
                            test = statementResult.summary().counters().nodesDeleted() == 1,
                            ifTrue = {},
                            ifFalse = { NotDeletedError(type = entityType.name, id = id) })
                }
            }

    fun exists(id: String, transaction: Transaction): Either<StoreError, Unit> =
            read("""MATCH (node:${entityType.name} {id: '$id'} ) RETURN count(node);""",
                    transaction) { statementResult ->
                Either.cond(
                        test = statementResult.single()["count(node)"].asInt(0) == 1,
                        ifTrue = {},
                        ifFalse = { NotFoundError(type = entityType.name, id = id) })
            }

    private fun doNotExist(id: String, transaction: Transaction): Either<StoreError, Unit> =
            read("""MATCH (node:${entityType.name} {id: '$id'} ) RETURN count(node);""",
                    transaction) { statementResult ->
                Either.cond(
                        test = statementResult.single()["count(node)"].asInt(1) == 0,
                        ifTrue = {},
                        ifFalse = { AlreadyExistsError(type = entityType.name, id = id) })
            }
}

sealed class BaseRelationStore

// TODO vihang: check if relation already exists, with allow duplicate boolean flag param
class RelationStore<FROM : HasId, RELATION, TO : HasId>(private val relationType: RelationType<FROM, RELATION, TO>) : BaseRelationStore() {

    init {
        relationType.relationStore = this
    }

    fun create(from: FROM, relation: RELATION, to: TO, transaction: Transaction): Either<StoreError, Unit> {

        val properties = getProperties(relation as Any)
        val strProps: String = properties.entries.joinToString(",") { """`${it.key}`: "${it.value}"""" }
        return write("""
                    MATCH (from:${relationType.from.name} { id: '${from.id}' }),(to:${relationType.to.name} { id: '${to.id}' })
                    CREATE (from)-[:${relationType.name} { $strProps } ]->(to);
                    """.trimIndent(),
                transaction) { statementResult ->

            // TODO vihang: validate if 'from' and 'to' node exists
            Either.cond(
                    test = statementResult.summary().counters().relationshipsCreated() == 1,
                    ifTrue = {},
                    ifFalse = { NotCreatedError(type = relationType.name, id = "${from.id} -> ${to.id}") })
        }
    }

    fun create(from: FROM, to: TO, transaction: Transaction): Either<StoreError, Unit> = write("""
                MATCH (from:${relationType.from.name} { id: '${from.id}' }),(to:${relationType.to.name} { id: '${to.id}' })
                CREATE (from)-[:${relationType.name}]->(to);
                """.trimIndent(),
            transaction) { statementResult ->

        // TODO vihang: validate if 'from' and 'to' node exists
        Either.cond(
                test = statementResult.summary().counters().relationshipsCreated() == 1,
                ifTrue = {},
                ifFalse = { NotCreatedError(type = relationType.name, id = "${from.id} -> ${to.id}") })
    }

    fun create(fromId: String, toId: String, transaction: Transaction): Either<StoreError, Unit> = write("""
                MATCH (from:${relationType.from.name} { id: '$fromId' }),(to:${relationType.to.name} { id: '$toId' })
                CREATE (from)-[:${relationType.name}]->(to);
                """.trimIndent(),
            transaction) { statementResult ->

        // TODO vihang: validate if 'from' and 'to' node exists
        Either.cond(
                test = statementResult.summary().counters().relationshipsCreated() == 1,
                ifTrue = {},
                ifFalse = { NotCreatedError(type = relationType.name, id = "$fromId -> $toId") })
    }

    fun create(fromId: String, relation: RELATION, toId: String, transaction: Transaction): Either<StoreError, Unit> {

        val properties = getProperties(relation as Any)
        val strProps: String = properties.entries.joinToString(",") { """`${it.key}`: "${it.value}"""" }
        return write("""
                MATCH (from:${relationType.from.name} { id: '$fromId' }),(to:${relationType.to.name} { id: '$toId' })
                CREATE (from)-[:${relationType.name} { $strProps } ]->(to);
                """.trimIndent(),
                transaction) { statementResult ->

            // TODO vihang: validate if 'from' and 'to' node exists
            Either.cond(
                    test = statementResult.summary().counters().relationshipsCreated() == 1,
                    ifTrue = {},
                    ifFalse = { NotCreatedError(type = relationType.name, id = "$fromId -> $toId") })
        }
    }

    fun create(fromId: String, toIds: Collection<String>, transaction: Transaction): Either<StoreError, Unit> = write("""
                MATCH (to:${relationType.to.name})
                WHERE to.id in [${toIds.joinToString(",") { "'$it'" }}]
                WITH to
                MATCH (from:${relationType.from.name} { id: '$fromId' })
                CREATE (from)-[:${relationType.name}]->(to);
                """.trimIndent(),
            transaction) { statementResult ->
        // TODO vihang: validate if 'from' and 'to' node exists
        val actualCount = statementResult.summary().counters().relationshipsCreated()
        Either.cond(
                test = actualCount == toIds.size,
                ifTrue = {},
                ifFalse = {
                    NotCreatedError(
                            type = relationType.name,
                            expectedCount = toIds.size,
                            actualCount = actualCount)
                })
    }

    fun create(fromIds: Collection<String>, toId: String, transaction: Transaction): Either<StoreError, Unit> = write("""
                MATCH (from:${relationType.from.name})
                WHERE from.id in [${fromIds.joinToString(",") { "'$it'" }}]
                WITH from
                MATCH (to:${relationType.to.name} { id: '$toId' })
                CREATE (from)-[:${relationType.name}]->(to);
                """.trimIndent(),
            transaction) { statementResult ->

        // TODO vihang: validate if 'from' and 'to' node exists
        val actualCount = statementResult.summary().counters().relationshipsCreated()
        Either.cond(
                test = actualCount == fromIds.size,
                ifTrue = {},
                ifFalse = {
                    NotCreatedError(
                            type = relationType.name,
                            expectedCount = fromIds.size,
                            actualCount = actualCount)
                })
    }

    fun removeAll(toId: String, transaction: Transaction): Either<StoreError, Unit> = write("""
                MATCH (from:${relationType.from.name})-[r:${relationType.name}]->(to:${relationType.to.name} { id: '$toId' })
                DELETE r;
                """.trimIndent(),
            transaction) {
        // TODO vihang: validate if 'to' node exists
        Unit.right()
    }
}

class ChangeableRelationStore<FROM : HasId, RELATION : HasId, TO : HasId>(private val relationType: RelationType<FROM, RELATION, TO>) : BaseRelationStore() {

    init {
        relationType.relationStore = this
    }

    fun get(id: String, transaction: Transaction): Either<StoreError, RELATION> {
        return read("""MATCH (from)-[r:${relationType.name}{id:'$id'}]->(to) RETURN r;""",
                transaction) { statementResult ->
            if (statementResult.hasNext()) {
                relationType.createRelation(statementResult.single().get("r").asMap()).right()
            } else {
                Either.left(NotFoundError(type = relationType.name, id = id))
            }
        }
    }

    fun update(relation: RELATION, transaction: Transaction): Either<StoreError, Unit> {
        val properties = getProperties(relation)
        // TODO vihang: replace setClause with map based settings written by Kjell
        val setClause: String = properties.entries.fold("") { acc, entry -> """$acc SET r.`${entry.key}` = "${entry.value}" """ }
        return write("""MATCH (from)-[r:${relationType.name}{id:'${relation.id}'}]->(to) $setClause ;""",
                transaction) { statementResult ->
            Either.cond(
                    test = statementResult.summary().counters().containsUpdates(), // TODO vihang: this is not perfect way to check if updates are applied
                    ifTrue = {},
                    ifFalse = { NotUpdatedError(type = relationType.name, id = relation.id) })
        }
    }
}

// Removes double apostrophes from key values in a JSON string.
// Usage: output = re.replace(input, "$1$2$3")
val re = Regex("""([,{])\s*"([^"]+)"\s*(:)""")

class UniqueRelationStore<FROM : HasId, RELATION, TO : HasId>(private val relationType: RelationType<FROM, RELATION, TO>) : BaseRelationStore() {

    init {
        relationType.relationStore = this
    }

    // If relation does not exists, then it creates new relation.
    fun createIfAbsent(fromId: String, toId: String, transaction: Transaction): Either<StoreError, Unit> {

        return (relationType.from.entityStore?.exists(fromId, transaction)
                ?: NotFoundError(type = relationType.from.name, id = fromId).left())
                .flatMap {
                    relationType.to.entityStore?.exists(toId, transaction)
                            ?: NotFoundError(type = relationType.to.name, id = toId).left()
                }.flatMap {

                    doNotExist(fromId, toId, transaction).fold(
                            { Unit.right() },
                            {
                                write("""
                                    MATCH (fromId:${relationType.from.name} {id: '$fromId'}),(toId:${relationType.to.name} {id: '$toId'})
                                    MERGE (fromId)-[:${relationType.name}]->(toId)
                                    """.trimMargin(),
                                        transaction) { statementResult ->

                                    Either.cond(statementResult.summary().counters().relationshipsCreated() == 1,
                                            ifTrue = { Unit },
                                            ifFalse = { NotCreatedError(relationType.name, "$fromId -> $toId") })
                                }
                            })
                }
    }

    // If relation does not exists, then it creates new relation.
    fun createIfAbsent(fromId: String, relation: RELATION, toId: String, transaction: Transaction): Either<StoreError, Unit> {

        return (relationType.from.entityStore?.exists(fromId, transaction)
                ?: NotFoundError(type = relationType.from.name, id = fromId).left())
                .flatMap {
                    relationType.to.entityStore?.exists(toId, transaction)
                            ?: NotFoundError(type = relationType.to.name, id = toId).left()
                }.flatMap {

                    doNotExist(fromId, toId, transaction).fold(
                            { Unit.right() },
                            {
                                val properties = getProperties(relation as Any)
                                val strProps: String = properties.entries.joinToString(",") { """`${it.key}`: "${it.value}"""" }

                                write("""
                                    MATCH (fromId:${relationType.from.name} {id: '$fromId'}),(toId:${relationType.to.name} {id: '$toId'})
                                    MERGE (fromId)-[:${relationType.name} { $strProps } ]->(toId)
                                    """.trimMargin(),
                                        transaction) { statementResult ->

                                    Either.cond(statementResult.summary().counters().relationshipsCreated() == 1,
                                            ifTrue = { Unit },
                                            ifFalse = { NotCreatedError(relationType.name, "$fromId -> $toId") })
                                }
                            })
                }
    }

    // If relation does not exists, then it creates new relation. Or else updates it.
    fun createOrUpdate(fromId: String, relation: RELATION, toId: String, transaction: Transaction): Either<StoreError, Unit> {

        return (relationType.from.entityStore?.exists(fromId, transaction)
                ?: NotFoundError(type = relationType.from.name, id = fromId).left())
                .flatMap {
                    relationType.to.entityStore?.exists(toId, transaction)
                            ?: NotFoundError(type = relationType.to.name, id = toId).left()
                }.flatMap {

                    val properties = getProperties(relation as Any)
                    doNotExist(fromId, toId, transaction).fold(
                            {
                                // TODO vihang: replace setClause with map based settings written by Kjell
                                val setClause: String = properties.entries.fold("") { acc, entry -> """$acc SET r.`${entry.key}` = '${entry.value}' """ }
                                write(
                                    """MATCH (fromId:${relationType.from.name} {id: '$fromId'})-[r:${relationType.name}]->(toId:${relationType.to.name} {id: '$toId'})
                                    $setClause ;""".trimMargin(),
                                        transaction) { statementResult ->
                                    Either.cond(
                                            test = statementResult.summary().counters().containsUpdates(), // TODO vihang: this is not perfect way to check if updates are applied
                                            ifTrue = {},
                                            ifFalse = { NotUpdatedError(type = relationType.name, id = "$fromId -> $toId") })
                                }
                            },
                            {
                                val strProps: String = properties.entries.joinToString(",") { """`${it.key}`: "${it.value}"""" }

                                write("""
                                MATCH (fromId:${relationType.from.name} {id: '$fromId'}),(toId:${relationType.to.name} {id: '$toId'})
                                MERGE (fromId)-[:${relationType.name} { $strProps } ]->(toId)
                                """.trimMargin(),
                                        transaction) { statementResult ->

                                    Either.cond(statementResult.summary().counters().relationshipsCreated() == 1,
                                            ifTrue = { Unit },
                                            ifFalse = { NotCreatedError(relationType.name, "$fromId -> $toId") })
                                }
                            })
                }
    }

    // If relation exists, then it fails with Already Exists Error, else it creates new relation.
    fun create(fromId: String, toId: String, transaction: Transaction): Either<StoreError, Unit> {

        return doNotExist(fromId, toId, transaction).flatMap {
            write("""
                        MATCH (fromId:${relationType.from.name} {id: '$fromId'}),(toId:${relationType.to.name} {id: '$toId'})
                        MERGE (fromId)-[:${relationType.name}]->(toId)
                        """.trimMargin(),
                    transaction) { statementResult ->

                Either.cond(statementResult.summary().counters().relationshipsCreated() == 1,
                        ifTrue = { Unit },
                        ifFalse = { NotCreatedError(relationType.name, "$fromId -> $toId") })
            }
        }
    }

    // If relation exists, then it fails with Already Exists Error, else it creates new relation.
    fun create(fromId: String, relation: RELATION, toId: String, transaction: Transaction): Either<StoreError, Unit> {

        return doNotExist(fromId, toId, transaction).flatMap {
            val properties = getProperties(relation as Any)
            val strProps: String = properties.entries.joinToString(",") { """`${it.key}`: "${it.value}"""" }

            write("""
                        MATCH (fromId:${relationType.from.name} {id: '$fromId'}),(toId:${relationType.to.name} {id: '$toId'})
                        MERGE (fromId)-[:${relationType.name}  { $strProps } ]->(toId)
                        """.trimMargin(),
                    transaction) { statementResult ->

                Either.cond(statementResult.summary().counters().relationshipsCreated() == 1,
                        ifTrue = { Unit },
                        ifFalse = { NotCreatedError(relationType.name, "$fromId -> $toId") })
            }
        }
    }

    fun delete(fromId: String, toId: String, transaction: Transaction): Either<StoreError, Unit> = write("""
                MATCH (:${relationType.from.name} { id: '$fromId'})-[r:${relationType.name}]->(:${relationType.to.name} {id: '$toId'})
                DELETE r
                """.trimMargin(),
            transaction) { statementResult ->

        Either.cond(statementResult.summary().counters().relationshipsDeleted() == 1,
                ifTrue = { Unit },
                ifFalse = { NotDeletedError(relationType.name, "$fromId -> $toId") })
    }

    fun get(fromId: String, toId: String, transaction: Transaction): Either<StoreError, RELATION> = read("""
                MATCH (:${relationType.from.name} {id: '$fromId'})-[r:${relationType.name}]->(:${relationType.to.name} {id: '$toId'})
                RETURN r
                """.trimMargin(),
            transaction) { statementResult ->

        Either.cond(statementResult.hasNext(),
                ifTrue = { relationType.createRelation(statementResult.single()["r"].asMap()) },
                ifFalse = { NotFoundError(relationType.name, "$fromId -> $toId") })
                .flatMap {
                    relation -> relation?.right() ?: NotFoundError(relationType.name, "$fromId -> $toId").left()
                }
    }

    private fun doNotExist(fromId: String, toId: String, transaction: Transaction): Either<StoreError, Unit> = read("""
                MATCH (:${relationType.from.name} {id: '$fromId'})-[r:${relationType.name}]->(:${relationType.to.name} {id: '$toId'})
                RETURN count(r)
                """.trimMargin(),
            transaction) { statementResult ->

        Either.cond(
                test = statementResult.single()["count(r)"].asInt(1) == 0,
                ifTrue = {},
                ifFalse = { AlreadyExistsError(type = relationType.name, id = "$fromId -> $toId") })

    }
}

//
// Helper wrapping Neo4j Client
//
object Graph {

    private val LOG by getLogger()

    private val trace by lazy { getResource<Trace>() }

    fun <R> write(query: String, transaction: Transaction, transform: (StatementResult) -> R): R {
        LOG.trace("write:[\n$query\n]")
        return trace.childSpan("neo4j.write") {
            transaction.run(query)
        }.let(transform)
    }

    fun <R> read(query: String, transaction: Transaction, transform: (StatementResult) -> R): R {
        LOG.trace("read:[\n$query\n]")
        return trace.childSpan("neo4j.read") {
            transaction.run(query)
        }.let(transform)
    }

    suspend fun <R> writeSuspended(query: String, transaction: Transaction, transform: (CompletionStage<StatementResultCursor>) -> R) {
        LOG.trace("write:[\n$query\n]")
        withContext(Dispatchers.Default) {
            trace.childSpan("neo4j.writeAsync") {
                transaction.runAsync(query)
            }
        }.let(transform)
    }
}

//
// Object mapping functions
//
object ObjectHandler {

    private const val SEPARATOR = '/'

    //
    // Object to Map
    //

    fun getProperties(any: Any): Map<String, Any> = toSimpleMap(
            objectMapper.convertValue(any, object : TypeReference<Map<String, Any?>>() {}))

    private fun toSimpleMap(map: Map<String, Any?>, prefix: String = ""): Map<String, Any> {
        val outputMap: MutableMap<String, Any> = LinkedHashMap()
        map.forEach { key, value ->
            when (value) {
                is Map<*, *> -> outputMap.putAll(toSimpleMap(value as Map<String, Any>, "$prefix$key$SEPARATOR"))
                is List<*> -> println("Skipping list value: $value for key: $key")
                null -> Unit
                else -> outputMap["$prefix$key"] = value
            }
        }
        return outputMap
    }

    //
    // Map to Object
    //

    fun <D> getObject(map: Map<String, Any>, dataClass: Class<D>): D {
        return objectMapper.convertValue(toNestedMap(map), dataClass)
    }

    internal fun toNestedMap(map: Map<String, Any>): Map<String, Any> {
        val outputMap: MutableMap<String, Any> = LinkedHashMap()
        map.forEach { key, value ->
            if (key.contains(SEPARATOR)) {
                val keys = key.split(SEPARATOR)
                var loopMap = outputMap
                repeat(keys.size - 1) { i ->
                    loopMap.putIfAbsent(keys[i], LinkedHashMap<String, Any>())
                    loopMap = loopMap[keys[i]] as MutableMap<String, Any>
                }
                loopMap[keys.last()] = value

            } else {
                outputMap[key] = value
            }
        }
        return outputMap
    }
}

// Need a dummy Void class with no-arg constructor to represent Relations with no properties.
class None