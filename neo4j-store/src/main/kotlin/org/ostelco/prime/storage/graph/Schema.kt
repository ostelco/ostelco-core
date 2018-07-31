package org.ostelco.prime.storage.graph

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.neo4j.driver.v1.AccessMode.READ
import org.neo4j.driver.v1.AccessMode.WRITE
import org.neo4j.driver.v1.Session
import org.neo4j.driver.v1.StatementResult
import org.neo4j.driver.v1.Transaction
import org.ostelco.prime.logger
import org.ostelco.prime.model.HasId
import org.ostelco.prime.storage.graph.Graph.read
import org.ostelco.prime.storage.graph.Graph.write
import org.ostelco.prime.storage.graph.ObjectHandler.getProperties

//
// Schema classes
//

data class EntityType<ENTITY : HasId>(
        private val dataClass: Class<ENTITY>,
        val name: String = dataClass.simpleName) {

    fun createEntity(map: Map<String, Any>): ENTITY = ObjectHandler.getObject(map, dataClass)
}

data class RelationType<FROM : HasId, RELATION, TO : HasId>(
        val relation: Relation,
        val from: EntityType<FROM>,
        val to: EntityType<TO>,
        private val dataClass: Class<RELATION>) {

    fun createRelation(map: Map<String, Any>): RELATION? {
        return ObjectHandler.getObject(map, dataClass)
    }
}

class EntityStore<E : HasId>(private val entityType: EntityType<E>) {

    fun getAll(transaction: Transaction): Map<String, E> =
            read("""MATCH (node:${entityType.name}) RETURN node;""", transaction) {
                it.list { entityType.createEntity(it["node"].asMap()) }
                        .mapNotNull { it.id to it }
                        .toMap()
            }


    fun get(id: String, transaction: Transaction): E? {
        return read("""MATCH (node:${entityType.name} {id: '$id'}) RETURN node;""", transaction) {
            if (it.hasNext())
                entityType.createEntity(it.single().get("node").asMap())
            else
                null
        }
    }

    fun create(entity: E, transaction: Transaction): Boolean {

        if (get(entity.id, transaction) != null) {
            return false
        }

        val properties = getProperties(entity)
        val strProps: String = properties.entries.joinToString(separator = ",") { """ `${it.key}`: "${it.value}"""" }
                .let { if (it.isNotBlank()) ",$it" else it }
        return write("""CREATE (node:${entityType.name} { id:"${entity.id}"$strProps });""",
                transaction) {
            it.summary().counters().nodesCreated() == 1
        }
    }

    fun <TO : HasId> getRelated(id: String, relationType: RelationType<E, *, TO>, transaction: Transaction): List<TO> {
        return read("""
                MATCH (:${relationType.from.name} {id: '$id'})-[:${relationType.relation.name}]->(node:${relationType.to.name})
                RETURN node;
                """.trimIndent(),
                transaction) {
            it.list { relationType.to.createEntity(it["node"].asMap()) }
        }
    }

    fun <FROM : HasId> getRelatedFrom(id: String, relationType: RelationType<FROM, *, E>, transaction: Transaction): List<FROM> {
        return read("""
                MATCH (node:${relationType.from.name})-[:${relationType.relation.name}]->(:${relationType.to.name} {id: '$id'})
                RETURN node;
                """.trimIndent(),
                transaction) {
            it.list { relationType.from.createEntity(it["node"].asMap()) }
        }
    }

    fun <RELATION : Any> getRelations(id: String, relationType: RelationType<E, RELATION, *>, transaction: Transaction): List<RELATION> {
        return read("""
                MATCH (from:${entityType.name} { id: '$id' })-[r:${relationType.relation.name}]-()
                return r;
                """.trimIndent(),
                transaction) {
            it.list { relationType.createRelation(it["r"].asMap()) }
                    .filterNotNull()
        }
    }

    fun update(entity: E, transaction: Transaction): Boolean {
        val properties = getProperties(entity)
        val setClause: String = properties.entries.fold("") { acc, entry -> """$acc SET node.${entry.key} = "${entry.value}" """ }
        return write("""MATCH (node:${entityType.name} { id: '${entity.id}' }) $setClause ;""",
                transaction) {
            it.summary().counters().containsUpdates()
        }
    }

    fun delete(id: String, transaction: Transaction): Boolean =
            write("""MATCH (node:${entityType.name} {id: '$id'} ) DELETE node;""",
                    transaction) {
                it.summary().counters().nodesDeleted() > 0
            }

}

class RelationStore<FROM : HasId, TO : HasId>(private val relationType: RelationType<FROM, *, TO>) {

    fun create(from: FROM, relation: Any, to: TO, transaction: Transaction): Boolean {

        val properties = getProperties(relation)
        val strProps: String = properties.entries.joinToString(",") { """`${it.key}`: "${it.value}"""" }
        return write("""
                MATCH (from:${relationType.from.name} { id: '${from.id}' }),(to:${relationType.to.name} { id: '${to.id}' })
                CREATE (from)-[:${relationType.relation.name} { $strProps } ]->(to);
                """.trimIndent(),
                transaction) {
            it.summary().counters().relationshipsCreated() == 1
        }
    }

    fun create(from: FROM, to: TO, transaction: Transaction): Boolean = write("""
                MATCH (from:${relationType.from.name} { id: '${from.id}' }),(to:${relationType.to.name} { id: '${to.id}' })
                CREATE (from)-[:${relationType.relation.name}]->(to);
                """.trimIndent(),
            transaction) {
        it.summary().counters().relationshipsCreated() == 1
    }

    fun create(fromId: String, toId: String, transaction: Transaction): Boolean = write("""
                MATCH (from:${relationType.from.name} { id: '$fromId' }),(to:${relationType.to.name} { id: '$toId' })
                CREATE (from)-[:${relationType.relation.name}]->(to);
                """.trimIndent(),
            transaction) {
        it.summary().counters().relationshipsCreated() == 1
    }

    fun create(fromId: String, relation: Any, toId: String, transaction: Transaction): Boolean = write("""
                MATCH (from:${relationType.from.name} { id: '$fromId' }),(to:${relationType.to.name} { id: '$toId' })
                CREATE (from)-[:${relationType.relation.name}]->(to);
                """.trimIndent(),
            transaction) {
        it.summary().counters().relationshipsCreated() == 1
    }

    fun create(fromId: String, toIds: Collection<String>, transaction: Transaction): Boolean = write("""
                MATCH (to:${relationType.to.name})
                WHERE to.id in [${toIds.joinToString(",") { "'$it'" }}]
                WITH to
                MATCH (from:${relationType.from.name} { id: '$fromId' })
                CREATE (from)-[:${relationType.relation.name}]->(to);
                """.trimIndent(),
            transaction) {
        it.summary().counters().relationshipsCreated() == toIds.size
    }

    fun create(fromIds: Collection<String>, toId: String, transaction: Transaction): Boolean = write("""
                MATCH (from:${relationType.from.name})
                WHERE from.id in [${fromIds.joinToString(",") { "'$it'" }}]
                WITH from
                MATCH (to:${relationType.to.name} { id: '$toId' })
                CREATE (from)-[:${relationType.relation.name}]->(to);
                """.trimIndent(),
            transaction) {
        it.summary().counters().relationshipsCreated() == fromIds.size
    }
}

//
// Helper wrapping Neo4j Client
//
object Graph {

    private val LOG by logger()

    fun <R> write(query: String, transaction: Transaction, transform: (StatementResult) -> R): R {
        LOG.debug("write:[\n$query\n]")
        return transaction.run(query).let(transform)
    }

    fun <R> read(query: String, transaction: Transaction, transform: (StatementResult) -> R): R {
        LOG.debug("read:[\n$query\n]")
        return transaction.run(query).let(transform)
    }
}

fun <R> readTransaction(action: ReadTransaction.() -> R): R {
    val session: Session = Neo4jClient.driver.session(READ)
    return session.use {
        it.readTransaction {
            action(ReadTransaction(it))
        }
    }
}

fun <R> writeTransaction(action: WriteTransaction.() -> R): R {
    val session: Session = Neo4jClient.driver.session(WRITE)
    return session.use {
        it.writeTransaction {
            action(WriteTransaction(it))
        }
    }
}

data class ReadTransaction(val transaction: Transaction)
data class WriteTransaction(val transaction: Transaction)

//
// Object mapping functions
//
object ObjectHandler {

    private const val SEPARATOR = '/'

    private val objectMapper = ObjectMapper().registerKotlinModule()

    //
    // Object to Map
    //

    fun getProperties(any: Any): Map<String, Any> = toSimpleMap(
            objectMapper.convertValue(any, object : TypeReference<Map<String, Any>>() {}))

    private fun toSimpleMap(map: Map<String, Any>, prefix: String = ""): Map<String, Any> {
        val outputMap: MutableMap<String, Any> = LinkedHashMap()
        map.forEach { key, value ->
            when (value) {
                is Map<*, *> -> outputMap.putAll(toSimpleMap(value as Map<String, Any>, "$prefix$key$SEPARATOR"))
                is List<*> -> println("Skipping list value: $value for key: $key")
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
                for (i in 0..(keys.size - 2)) {
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