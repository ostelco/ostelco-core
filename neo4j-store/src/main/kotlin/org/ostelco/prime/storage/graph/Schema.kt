package org.ostelco.prime.storage.graph

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.RelationshipType
import org.neo4j.graphdb.Transaction
import org.ostelco.prime.logger
import org.ostelco.prime.model.HasId
import org.ostelco.prime.storage.graph.ObjectHandler.getProperties
import java.util.stream.Collectors

//
// Schema classes
//

data class EntityType<ENTITY : HasId>(
        val name: String,
        private val dataClass: Class<ENTITY>) {

    fun createEntity(map: Map<String, Any>): ENTITY = ObjectHandler.getObject(map, dataClass)
}

data class RelationType<FROM : HasId, RELATION, TO : HasId>(
        val name: String,
        val from: EntityType<FROM>,
        val to: EntityType<TO>,
        private val dataClass: Class<RELATION>?) {

    fun createRelation(map: Map<String, Any>): RELATION? {
        return ObjectHandler.getObject(map, dataClass ?: return null)
    }
}

class EntityStore<E : HasId>(private val entityType: EntityType<E>) {

    private val LOG by logger()

    fun getAll(): Map<String, E> {
        return Graph
                .findNodes(Label.label(entityType.name))
                .stream()
                .collect(Collectors.toMap(
                        { it.getProperty("id") as String },
                        { entityType.createEntity(it.allProperties) }))

    }

    fun get(id: String): E? = getNode(id)?.let { entityType.createEntity(it.allProperties) }

    fun getNode(id: String): Node? = entityType.name.getGraphNode(id)

    fun create(id: String, entity: E): Boolean {
        var transaction: Transaction? = null
        try {
            transaction = Graph.beginTx()
            val node = Graph.createNode(Label.label(entityType.name))
            getProperties(entity).forEach {
                node.setProperty(it.key, it.value)
            }
            node.setProperty("id", id)
            transaction.success()
            return true
        } catch (e: Exception) {
            LOG.error("Failed to create ${entityType.name}", e)
            transaction?.failure()
            return false
        }
    }

    fun <TO : HasId> getRelated(id: String, relationType: RelationType<E, *, TO>, toEntityType: EntityType<TO>): List<TO> {
        var transaction: Transaction? = null
        try {
            transaction = Graph.beginTx()
            return entityType.name.getGraphRelatedNodes(id, relationType.name)
                    .map { toEntityType.createEntity(it.allProperties) }
        } finally {
            transaction?.close()
        }
    }

    fun <RELATION : Any> getRelations(id: String, relationType: RelationType<E, RELATION, *>): List<RELATION> {
        return entityType.name.getGraphRelations(id, relationType.name)
                .mapNotNull { relationType.createRelation(it.allProperties) }
    }

    fun update(id: String, entity: E): Boolean {
        var transaction: Transaction? = null
        try {
            transaction = Graph.beginTx()
            val node = getNode(id) ?: return false
            getProperties(entity).forEach {
                node.setProperty(it.key, it.value)
            }
            transaction.success()
            return true
        } catch (e: Exception) {
            LOG.error("Failed to create ${entityType.name}", e)
            transaction?.failure()
            return false
        } finally {
            transaction?.close()
        }
    }

    fun delete(id: String): Boolean {
        val node = getNode(id) ?: return false
        node.delete()
        return true
    }
}

class RelationStore<FROM : HasId, TO : HasId>(private val relationType: RelationType<FROM, *, TO>) {

    private val LOG by logger()

    fun create(from: FROM, relation: Any?, to: TO): Boolean {
        var transaction: Transaction? = null
        try {
            transaction = Graph.beginTx()
            val fromNode = relationType.from.name.getGraphNode(from.id) ?: return false
            val toNode = relationType.to.name.getGraphNode(to.id) ?: return false

            val relationship = fromNode.createRelationshipTo(toNode, RelationshipType.withName(relationType.name))
            if (relation != null) {
                getProperties(relation).forEach {
                    relationship.setProperty(it.key, it.value)
                }
            }
            transaction.success()
            return true
        } catch (e: Exception) {
            LOG.error("Failed to create ${relationType.name}", e)
            transaction?.failure()
            return false
        } finally {
            transaction?.close()
        }
    }

    fun create(fromId: String, toIds: Collection<String>): Boolean =
            relationType.from.name.createRelationsTo(
                    fromId = fromId,
                    relation = relationType.name,
                    toLabel = relationType.to.name,
                    toIds = toIds)

}

//
// String extension functions
//

fun String.getGraphNode(id: String): Node? = Graph.findNode(Label.label(this), "id", id)

fun String.getGraphRelations(id: String, relation: String): Collection<Relationship> {
    return this.getGraphNode(id)
            ?.getRelationships(RelationshipType.withName(relation))
            ?.toList() ?: emptyList()
}

fun String.getGraphRelatedNodes(id: String, relation: String): Collection<Node> {
    return this.getGraphRelations(id, relation)
            .map { it.endNode }
}

fun String.createRelationsTo(fromId: String, relation: String, toLabel: String, toIds: Collection<String>): Boolean {

    var transaction: Transaction? = null
    try {
        transaction = Graph.beginTx()
        val fromNode = getGraphNode(fromId) ?: return false
        val relationType = RelationshipType.withName(relation)
        // delete existing relations. So, this function can be used for update too.
        fromNode.getRelationships(relationType).forEach { it.delete() }
        toIds.map { toLabel.getGraphNode(it) }
                .map { fromNode.createRelationshipTo(it, relationType) }
        transaction.success()
        return true
    } catch (e: Exception) {
        val logger by logger()
        logger.error("Failed to create $relation", e)
        transaction?.failure()
        return false
    }
}

//
// Object mapping functions
//
object ObjectHandler {

    private const val SEPARATOR = '/'

    private val objectMapper = ObjectMapper().registerKotlinModule()

    fun getProperties(any: Any): Map<String, Any> = toSimpleMap(
            objectMapper.convertValue(any, object : TypeReference<Map<String, Any>>() {}))

    private fun toSimpleMap(map: Map<String, Any>, prefix: String = ""): Map<String, Any> {
        val outputMap: MutableMap<String, Any> = LinkedHashMap()
        map.forEach { key, value ->
            when (value) {
                is Map<*, *> -> outputMap.putAll(toSimpleMap(value as Map<String, Any>, "$prefix$key$SEPARATOR"))
                is List<*> -> println("Skipping list value: $value")
                else -> outputMap["$prefix$key"] = value
            }
        }
        return outputMap
    }

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