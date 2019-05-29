package org.ostelco.prime.storage.graph

import org.ostelco.prime.model.HasId
import org.ostelco.prime.storage.graph.EntityRegistry.getEntityStore
import org.ostelco.prime.storage.graph.EntityRegistry.getEntityType
import kotlin.reflect.KClass

object EntityRegistry {

    private val entityTypeMap = mutableMapOf<Class<out HasId>, EntityType<out HasId>>()

    fun <E : HasId> getEntityType(clazz: Class<E>): EntityType<E> {
        return (entityTypeMap as MutableMap<Class<E>, EntityType<E>>).getOrPut(clazz) {
            val entityType = EntityType(clazz)
            EntityStore(entityType)
            entityType
        }
    }

    fun <E : HasId> getEntityStore(clazz: Class<E>): EntityStore<E> =
            getEntityType(clazz).entityStore ?: throw Exception("Missing EntityStore for Entity Type: ${clazz.name}")
}

val <E : HasId> KClass<E>.entityType: EntityType<E>
    get() = getEntityType(this.java)

val <E : HasId> KClass<E>.entityStore: EntityStore<E>
    get() = getEntityStore(this.java)

object RelationRegistry {

    private val relationTypeMap = mutableMapOf<Relation, RelationType<out HasId, *, out HasId>>()
    private val relationStoreMap = mutableMapOf<Relation, RelationStore<out HasId, *, out HasId>>()

    private val relationFromTypeMap = mutableMapOf<KClass<out HasId>, RelationType<out HasId, *, out HasId>>()
    private val relationToTypeMap = mutableMapOf<KClass<out HasId>, RelationType<out HasId, *, out HasId>>()

    fun <FROM : HasId, RELATION, TO : HasId> register(relation: Relation, relationType: RelationType<FROM, RELATION, TO>) {
        relationTypeMap[relation] = relationType
        relationFromTypeMap[relation.from] = relationType
        relationToTypeMap[relation.to] = relationType
    }

    fun <FROM : HasId, RELATION, TO : HasId> register(relation: Relation, relationStore: RelationStore<FROM, RELATION, TO>) {
        relationStoreMap[relation] = relationStore
    }

    fun getRelationType(relation: Relation) = relationTypeMap[relation]

    fun <FROM : HasId> getRelationTypeFrom(from: KClass<FROM>) = relationFromTypeMap[from]

    fun <TO : HasId> getRelationTypeTo(to: KClass<TO>) = relationToTypeMap[to]
}