package org.ostelco.prime.storage.graph

import org.ostelco.prime.model.HasId
import org.ostelco.prime.storage.graph.EntityRegistry.getEntityStore
import org.ostelco.prime.storage.graph.EntityRegistry.getEntityType
import kotlin.reflect.KClass

object EntityRegistry {

    private val entityTypeMap = mutableMapOf<KClass<out HasId>, EntityType<out HasId>>()

    fun <E : HasId> getEntityType(kClass: KClass<E>): EntityType<E> {
        return (entityTypeMap as MutableMap<KClass<E>, EntityType<E>>).getOrPut(kClass) {
            val entityType = EntityType(kClass.java)
            EntityStore(entityType)
            entityType
        }
    }

    fun <E : HasId> getEntityStore(kClass: KClass<E>): EntityStore<E> = getEntityType(kClass).entityStore
}

val <E : HasId> KClass<E>.entityType: EntityType<E>
    get() = getEntityType(this)

val <E : HasId> KClass<E>.entityStore: EntityStore<E>
    get() = getEntityStore(this)
