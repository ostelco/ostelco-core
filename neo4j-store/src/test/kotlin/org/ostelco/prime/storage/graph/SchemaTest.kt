package org.ostelco.prime.storage.graph

import com.fasterxml.jackson.core.type.TypeReference
import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks
import org.joda.time.Duration
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.neo4j.driver.v1.AccessMode.WRITE
import org.ostelco.prime.dsl.EntityContext
import org.ostelco.prime.dsl.writeTransaction
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.kts.engine.KtsServiceFactory
import org.ostelco.prime.kts.engine.reader.ClasspathResourceTextReader
import org.ostelco.prime.model.HasId
import org.ostelco.prime.storage.AlreadyExistsError
import org.ostelco.prime.storage.graph.Relation.REFERRED
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.fail

class SchemaTest {

    @BeforeTest
    fun clear() {

        Neo4jClient.driver.session(WRITE).use { session ->
            session.writeTransaction { transaction ->
                transaction.run("MATCH (n) DETACH DELETE n")
            }
        }
    }

    @Test
    fun `test node`() {
        writeTransaction {
            val aId = "a_id"
            val aEntity = EntityType(A::class.java)
            val aEntityStore = EntityStore(aEntity)

            // create node
            val a = A()
            a.id = aId
            a.field1 = "value1"
            a.field2 = "value2"

            aEntityStore.create(a, transaction)

            // get node
            assertEquals(a, get(EntityContext(A::class, "a_id")).toOption().orNull())

            // update node
            val ua = A()
            ua.id = aId
            ua.field1 = "value1_u"
            ua.field2 = "value2_u"

            aEntityStore.update(ua, transaction)

            // get updated node
            assertEquals(ua, get(EntityContext(A::class, aId)).toOption().orNull())

            // delete node
            aEntityStore.delete(aId, transaction)

            // get deleted node
            assert(get(EntityContext(A::class, aId)).isLeft())
        }
    }

    @Test
    fun `test related node`() {

        writeTransaction {
            val aId = "a_id"
            val bId = "b_id"

            val fromEntity = EntityType(A::class.java)
            val fromEntityStore = EntityStore(fromEntity)

            val toEntity = EntityType(B::class.java)
            val toEntityStore = EntityStore(toEntity)

            val relation = RelationType(REFERRED, fromEntity, toEntity, Void::class.java)
            val relationStore = RelationStore(relation)

            // create nodes
            val a = A()
            a.id = aId
            a.field1 = "a's value1"
            a.field2 = "a's value2"

            val b = B()
            b.id = bId
            b.field1 = "b's value1"
            b.field2 = "b's value2"

            fromEntityStore.create(a, transaction)
            toEntityStore.create(b, transaction)

            // create relation
            relationStore.create(a, b, transaction)

            // get 'b' from 'a'
            fromEntityStore.getRelated(aId, relation, transaction).bimap(
                    { fail(it.message) },
                    { assertEquals(listOf(b), it) })
        }
    }

    @Test
    fun `test relation with properties`() {

        writeTransaction {
            val aId = "a_id"
            val bId = "b_id"

            val fromEntity = EntityType(A::class.java)
            val fromEntityStore = EntityStore(fromEntity)

            val toEntity = EntityType(B::class.java)
            val toEntityStore = EntityStore(toEntity)

            val relation = RelationType(REFERRED, fromEntity, toEntity, R::class.java)
            val relationStore = RelationStore(relation)

            // create nodes
            val a = A()
            a.id = aId
            a.field1 = "a's value1"
            a.field2 = "a's value2"

            val b = B()
            b.id = bId
            b.field1 = "b's value1"
            b.field2 = "b's value2"

            fromEntityStore.create(a, transaction)
            toEntityStore.create(b, transaction)

            // create relation
            val r = R()
            r.field1 = "r's value1"
            r.field2 = "r's value2"
            relationStore.create(a, r, b, transaction)

            // get 'b' from 'a'
            fromEntityStore.getRelated(aId, relation, transaction).fold(
                    { fail(it.message) },
                    { assertEquals(listOf(b), it) })

            // get 'r' from 'a'
            fromEntityStore.getRelations(aId, relation, transaction).fold(
                    { fail(it.message) },
                    { assertEquals(listOf(r), it) })
        }
    }

    @Test
    fun `test fail to create relation due to missing node`() {
        val either = writeTransaction {
            val aId = "a_id"
            val bId = "b_id"

            val fromEntity = EntityType(A::class.java)

            val toEntity = EntityType(B::class.java)
            val toEntityStore = EntityStore(toEntity)

            val relation = RelationType(REFERRED, fromEntity, toEntity, Void::class.java)
            val relationStore = RelationStore(relation)

            // create node
            val b = B()
            b.id = bId
            b.field1 = "b's value1"
            b.field2 = "b's value2"

            toEntityStore.create(b, transaction)

            // create relation
            relationStore.create(aId, bId, transaction)
        }

        either.fold(
                { assertEquals("Failed to create REFERRED - a_id -> b_id", it.message) },
                { fail("Did not received error while creating relation for missing node") })
    }

    @Test
    fun `json to map`() {
        val map = objectMapper.readValue<Map<String, String>>("""{"label":"3GB for 300 NOK"}""", object : TypeReference<LinkedHashMap<String, String>>() {})
        assertEquals("3GB for 300 NOK", map["label"])
    }


    @Test
    fun `test unique relation store`() {

        writeTransaction {
            val aId = "a_id"
            val bId = "b_id"

            val fromEntity = EntityType(A::class.java)
            val fromEntityStore = EntityStore(fromEntity)

            val toEntity = EntityType(B::class.java)
            val toEntityStore = EntityStore(toEntity)

            val relation = RelationType(REFERRED, fromEntity, toEntity, R::class.java)
            val uniqueRelationStore = UniqueRelationStore(relation)

            // create nodes
            val a = A()
            a.id = aId
            a.field1 = "a's value1"
            a.field2 = "a's value2"

            val b = B()
            b.id = bId
            b.field1 = "b's value1"
            b.field2 = "b's value2"

            fromEntityStore.create(a, transaction)
            toEntityStore.create(b, transaction)

            // create relation
            uniqueRelationStore.create(a.id, b.id, transaction)
                    .mapLeft { fail(it.message) }
            uniqueRelationStore.createIfAbsent(a.id, b.id, transaction)
                    .mapLeft { fail(it.message) }
            uniqueRelationStore.createIfAbsent(a.id, b.id, transaction)
                    .mapLeft { fail(it.message) }
            uniqueRelationStore.create(a.id, b.id, transaction).fold(
                    { storeError -> assert(storeError is AlreadyExistsError) },
                    { fail("Created duplicate relation") })
        }
    }

    companion object {
        @ClassRule
        @JvmField
        var docker: DockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/docker-compose.yaml")
                .waitingForService("neo4j", HealthChecks.toHaveAllPortsOpen())
                .waitingForService("neo4j",
                        HealthChecks.toRespond2xxOverHttp(7474) { port ->
                            port.inFormat("http://\$HOST:\$EXTERNAL_PORT/browser")
                        },
                        Duration.standardSeconds(40L))
                .build()

        @BeforeClass
        @JvmStatic
        fun start() {
            ConfigRegistry.config = Config(
                    host = "0.0.0.0",
                    protocol = "bolt",
                    hssNameLookupService = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.HssNameLookupService",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/HssNameLookupService.kts"
                            )
                    ),
                    onNewCustomerAction = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.OnNewCustomerAction",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/OnNewCustomerAction.kts"
                            )
                    ),
                    allowedRegionsService = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.AllowedRegionsService",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/AllowedRegionsService.kts"
                            )
                    )
            )
            Neo4jClient.start()
        }

        @AfterClass
        @JvmStatic
        fun stop() {
            Neo4jClient.stop()
        }
    }
}

data class A(
        var field1: String? = null,
        var field2: String? = null) : HasId {

    private var _id: String = ""

    override var id: String
        get() = _id
        set(value) {
            _id = value
        }
}

data class B(
        var field1: String? = null,
        var field2: String? = null) : HasId {

    private var _id: String = ""

    override var id: String
        get() = _id
        set(value) {
            _id = value
        }
}

data class R(
        var field1: String? = null,
        var field2: String? = null) : HasId {

    private var _id: String = ""

    override var id: String
        get() = _id
        set(value) {
            _id = value
        }
}