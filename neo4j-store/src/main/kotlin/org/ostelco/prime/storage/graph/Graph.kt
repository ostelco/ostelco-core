package org.ostelco.prime.storage.graph

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.RelationshipType
import org.neo4j.graphdb.ResourceIterable
import org.neo4j.graphdb.ResourceIterator
import org.neo4j.graphdb.Result
import org.neo4j.graphdb.Transaction
import org.neo4j.graphdb.event.KernelEventHandler
import org.neo4j.graphdb.event.TransactionEventHandler
import org.neo4j.graphdb.index.IndexManager
import org.neo4j.graphdb.schema.Schema
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription
import org.neo4j.graphdb.traversal.TraversalDescription
import java.util.concurrent.TimeUnit

object Graph: GraphDatabaseService {

    override fun createNode(): Node {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createNode(vararg labels: Label?): Node {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> unregisterTransactionEventHandler(handler: TransactionEventHandler<T>?): TransactionEventHandler<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun index(): IndexManager {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bidirectionalTraversalDescription(): BidirectionalTraversalDescription {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun registerKernelEventHandler(handler: KernelEventHandler?): KernelEventHandler {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNodeById(id: Long): Node {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllLabels(): ResourceIterable<Label> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun beginTx(): Transaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun beginTx(timeout: Long, unit: TimeUnit?): Transaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllNodes(): ResourceIterable<Node> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllLabelsInUse(): ResourceIterable<Label> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllRelationshipTypes(): ResourceIterable<RelationshipType> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllRelationships(): ResourceIterable<Relationship> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findNodes(label: Label?, key: String?, value: Any?): ResourceIterator<Node> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findNodes(label: Label?): ResourceIterator<Node> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> registerTransactionEventHandler(handler: TransactionEventHandler<T>?): TransactionEventHandler<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createNodeId(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun traversalDescription(): TraversalDescription {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute(query: String?): Result {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute(query: String?, timeout: Long, unit: TimeUnit?): Result {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute(query: String?, parameters: MutableMap<String, Any>?): Result {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute(query: String?, parameters: MutableMap<String, Any>?, timeout: Long, unit: TimeUnit?): Result {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun shutdown() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRelationshipById(id: Long): Relationship {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findNode(label: Label?, key: String?, value: Any?): Node {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllPropertyKeys(): ResourceIterable<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unregisterKernelEventHandler(handler: KernelEventHandler?): KernelEventHandler {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun schema(): Schema {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isAvailable(timeout: Long): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllRelationshipTypesInUse(): ResourceIterable<RelationshipType> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

