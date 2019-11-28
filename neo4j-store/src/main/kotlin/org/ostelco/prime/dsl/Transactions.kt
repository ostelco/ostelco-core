package org.ostelco.prime.dsl

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import org.neo4j.driver.v1.AccessMode.READ
import org.neo4j.driver.v1.AccessMode.WRITE
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.HasId
import org.ostelco.prime.storage.DatabaseError
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.SystemError
import org.ostelco.prime.storage.graph.EntityRegistry
import org.ostelco.prime.storage.graph.EntityStore
import org.ostelco.prime.storage.graph.Neo4jClient
import org.ostelco.prime.storage.graph.PrimeTransaction
import org.ostelco.prime.storage.graph.RelationStore
import org.ostelco.prime.storage.graph.UniqueRelationStore
import kotlin.reflect.KClass

object DSL {

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

open class ReadTransaction(open val transaction: PrimeTransaction) {

    fun <E : HasId> get(entityContext: EntityContext<E>): Either<StoreError, E> {
        val entityStore: EntityStore<E> = EntityRegistry.getEntityStore(entityContext.entityClass)
        return entityStore.get(id = entityContext.id, transaction = transaction)
    }

    fun <FROM : HasId, TO : HasId> get(relatedToClause: RelatedToClause<FROM, TO>): Either<StoreError, List<FROM>> {
        val entityStore: EntityStore<TO> = relatedToClause.relationType.to.entityStore
                ?: return DatabaseError(type = "entityStore", id = relatedToClause.relationType.to.name, message = "Missing entity store").left()
        return entityStore.getRelatedFrom(
                id = relatedToClause.toId,
                relationType = relatedToClause.relationType,
                transaction = transaction)
    }

    fun <FROM : HasId, TO : HasId> get(relatedFromClause: RelatedFromClause<FROM, TO>): Either<StoreError, List<TO>> {
        val entityStore: EntityStore<FROM> = relatedFromClause.relationType.from.entityStore
                ?: return DatabaseError(type = "entityStore", id = relatedFromClause.relationType.from.name, message = "Missing entity store").left()
        return entityStore.getRelated(
                id = relatedFromClause.fromId,
                relationType = relatedFromClause.relationType,
                transaction = transaction)
    }

    fun <FROM : HasId, RELATION : Any> get(relationFromClause: RelationFromClause<FROM, RELATION, *>): Either<StoreError, List<RELATION>> {
        val entityStore: EntityStore<FROM> = relationFromClause.relationType.from.entityStore
                ?: return DatabaseError(type = "entityStore", id = relationFromClause.relationType.from.name, message = "Missing entity store").left()
        return entityStore.getRelations(
                id = relationFromClause.fromId,
                relationType = relationFromClause.relationType,
                transaction = transaction)
    }
}

class WriteTransaction(override val transaction: PrimeTransaction) : ReadTransaction(transaction = transaction) {

    private val logger by getLogger()

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

    fun <E : HasId> delete(entityContext: EntityContext<E>): Either<StoreError, Unit> {
        val entityStore: EntityStore<E> = EntityRegistry.getEntityStore(entityContext.entityClass)
        return entityStore.delete(id = entityContext.id, transaction = transaction)
    }

    fun <FROM : HasId, RELATION, TO : HasId> fact(expression: () -> RelationExpression<FROM, RELATION, TO>): Either<StoreError, Unit> {
        val relationExpression = expression()
        val relationStore = relationExpression.relationType.relationStore
        val relation = relationExpression.relation
        return when (relationStore) {
            is RelationStore<*, *, *> -> {
                if (relation != null) {
                    (relationStore as RelationStore<*, RELATION, *>).create(
                            fromId = relationExpression.fromId,
                            toId = relationExpression.toId,
                            relation = relation,
                            transaction = transaction
                    )
                } else {
                    relationStore.create(
                            fromId = relationExpression.fromId,
                            toId = relationExpression.toId,
                            transaction = transaction
                    )
                }
            }
            is UniqueRelationStore<*, *, *> -> {
                if (relation != null) {
                    (relationStore as UniqueRelationStore<*, RELATION, *>).create(
                            fromId = relationExpression.fromId,
                            toId = relationExpression.toId,
                            relation = relation,
                            transaction = transaction
                    )
                } else {
                    relationStore.createIfAbsent(
                            fromId = relationExpression.fromId,
                            toId = relationExpression.toId,
                            transaction = transaction
                    )
                }
            }
            null -> {
                SystemError(type = "relationStore", id = relationExpression.relationType.name, message = "Missing relation store").left()
            }
        }
    }

    fun <FROM : HasId, RELATION, TO : HasId> unlink(expression: () -> RelationExpression<FROM, RELATION, TO>): Either<StoreError, Unit> {
        val relationExpression = expression()
        val relationStore = relationExpression.relationType.relationStore
        return relationStore?.delete(
                fromId = relationExpression.fromId,
                toId = relationExpression.toId,
                transaction = transaction
        ) ?: SystemError(
                type = "relationStore",
                id = relationExpression.relationType.name,
                message = "Missing relation store"
        ).left()
    }
}

class JobContext(private val transaction: PrimeTransaction) {

    var result: Either<StoreError, Unit> = Unit.right()

    fun <E : HasId> create(obj: () -> E) {
        result = result.flatMap {
            WriteTransaction(transaction).create(obj)
        }
    }

    fun <E : HasId> update(obj: () -> E) {
        result = result.flatMap {
            WriteTransaction(transaction).update(obj)
        }
    }

    fun <E : HasId> get(entityClass: KClass<E>, id: String): Either<StoreError, E> {
        return result.flatMap {
            val entityStore: EntityStore<E> = EntityRegistry.getEntityStore(entityClass)
            entityStore.get(id = id, transaction = transaction)
        }
    }

    fun <FROM : HasId, RELATION, TO : HasId> fact(expression: () -> RelationExpression<FROM, RELATION, TO>) {
        result = result.flatMap {
            WriteTransaction(transaction).fact(expression)
        }
    }

    fun <FROM : HasId, RELATION, TO : HasId> unlink(expression: () -> RelationExpression<FROM, RELATION, TO>) {
        result = result.flatMap {
            WriteTransaction(transaction).unlink(expression)
        }
    }
}
