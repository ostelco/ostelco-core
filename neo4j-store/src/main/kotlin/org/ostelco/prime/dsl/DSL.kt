package org.ostelco.prime.dsl

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import org.neo4j.driver.v1.AccessMode.READ
import org.neo4j.driver.v1.AccessMode.WRITE
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.HasId
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.SystemError
import org.ostelco.prime.storage.graph.ChangeableRelationStore
import org.ostelco.prime.storage.graph.EntityRegistry
import org.ostelco.prime.storage.graph.EntityStore
import org.ostelco.prime.storage.graph.Neo4jClient
import org.ostelco.prime.storage.graph.PrimeTransaction
import org.ostelco.prime.storage.graph.Relation
import org.ostelco.prime.storage.graph.RelationRegistry
import org.ostelco.prime.storage.graph.RelationStore
import org.ostelco.prime.storage.graph.UniqueRelationStore
import kotlin.reflect.KClass

object DSL {

    private val logger by getLogger()

    fun job(work: JobContext.() -> Unit): Either<StoreError, Unit> = writeTransaction {
        val jobContext = JobContext(transaction = transaction)
        work(jobContext)
        jobContext.result
    }
}

fun <R> readTransaction(action: ReadTransaction.() -> R): R =
        Neo4jClient.driver.session(READ)
                .use { session ->
                    session.readTransaction { transaction ->
                        val primeTransaction = PrimeTransaction(transaction)
                        val result = action(ReadTransaction(primeTransaction))
                        primeTransaction.close()
                        result
                    }
                }

fun <R> writeTransaction(action: WriteTransaction.() -> R): R =
        Neo4jClient.driver.session(WRITE)
                .use { session ->
                    session.writeTransaction { transaction ->
                        val primeTransaction = PrimeTransaction(transaction)
                        val result = action(WriteTransaction(primeTransaction))
                        primeTransaction.close()
                        result
                    }
                }

suspend fun <R> suspendedWriteTransaction(action: suspend WriteTransaction.() -> R): R =
        Neo4jClient.driver.session(WRITE)
                .use { session ->
                    val transaction = session.beginTransaction()
                    val primeTransaction = PrimeTransaction(transaction)
                    val result = action(WriteTransaction(primeTransaction))
                    primeTransaction.success()
                    primeTransaction.close()
                    transaction.close()
                    result
                }

data class ReadTransaction(val transaction: PrimeTransaction) {
    fun <E : HasId> get(entityClass: KClass<E>, id: String): Either<StoreError, E> {
        val entityStore: EntityStore<E> = EntityRegistry.getEntityStore(entityClass)
        return entityStore.get(id = id, transaction = transaction)
    }
}

data class WriteTransaction(val transaction: PrimeTransaction) {

    fun <E : HasId> create(obj: () -> E): Either<StoreError, Unit> {
        val entity: E = obj()
        val entityStore: EntityStore<E> = EntityRegistry.getEntityStore(entity::class) as EntityStore<E>
        return entityStore.create(entity = entity, transaction = transaction)
    }

    fun <E : HasId> update(obj: () -> E): Either<StoreError, Unit> {
        val entity: E = obj()
        val entityStore: EntityStore<E> = EntityRegistry.getEntityStore(entity::class) as EntityStore<E>
        return entityStore.update(entity = entity, transaction = transaction)
    }

    fun <E : HasId> get(entityClass: KClass<E>, id: String): Either<StoreError, E> {
        val entityStore: EntityStore<E> = EntityRegistry.getEntityStore(entityClass)
        return entityStore.get(id = id, transaction = transaction)
    }

    fun <E : HasId> delete(entityClass: KClass<E>, id: String): Either<StoreError, Unit> {
        val entityStore: EntityStore<E> = EntityRegistry.getEntityStore(entityClass)
        return entityStore.delete(id = id, transaction = transaction)
    }
}

class JobContext(private val transaction: PrimeTransaction) {

    var result: Either<StoreError, Unit> = Unit.right()

    fun <E : HasId> create(obj: () -> E) {
        result = result.flatMap {
            val entity: E = obj()
            val entityStore: EntityStore<E> = EntityRegistry.getEntityStore(entity::class) as EntityStore<E>
            entityStore.create(entity = entity, transaction = transaction)
        }
    }

    fun <E : HasId> update(obj: () -> E) {
        result = result.flatMap {
            val entity: E = obj()
            val entityStore: EntityStore<E> = EntityRegistry.getEntityStore(entity::class) as EntityStore<E>
            entityStore.update(entity = entity, transaction = transaction)
        }
    }

    fun <E : HasId> get(entityClass: KClass<E>, id: String): Either<StoreError, E> {
        return result.flatMap {
            val entityStore: EntityStore<E> = EntityRegistry.getEntityStore(entityClass)
            entityStore.get(id = id, transaction = transaction)
        }
    }

    fun fact(fact: () -> RelationContext) {
        val relationContext = fact()
        val relationType = RelationRegistry.getRelationType(relationContext.relation)
        when (val baseRelationStore = relationType?.relationStore) {
            null -> {
            }
            is RelationStore<*, *, *> -> {
                result = result.flatMap {
                    baseRelationStore.create(
                            fromId = relationContext.fromId,
                            toId = relationContext.toId,
                            transaction = transaction)
                }
            }
            is UniqueRelationStore<*, *, *> -> {
                result = result.flatMap {
                    baseRelationStore.create(
                            fromId = relationContext.fromId,
                            toId = relationContext.toId,
                            transaction = transaction)
                }
            }
            is ChangeableRelationStore<*, *, *> -> {
                result = result.flatMap {
                    SystemError(type = relationType.name, id = "", message = "Unable to create changable relation").left()
                }
            }
        }
    }
}

data class RelationContext(
        val fromId: String,
        val relation: Relation,
        val toId: String)