package org.ostelco.prime.storage.embeddedgraph

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.ostelco.prime.model.HasId
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SchemaTest {

    @Test
    fun `test node`() {

        val aId = "a_id"
        val aEntity = EntityType("A", A::class.java)
        val aEntityStore = EntityStore(aEntity)

        // create node
        val a = A()
        a.id = aId
        a.field1 = "value1"
        a.field2 = "value2"

        aEntityStore.create(aId, a)

        // get node
        assertEquals(a, aEntityStore.get("a_id"))

        // update node
        val ua = A()
        ua.id = aId
        ua.field1 = "value1_u"
        ua.field2 = "value2_u"

        aEntityStore.update(aId, ua)

        // get updated node
        assertEquals(ua, aEntityStore.get(aId))

        // delete node
        aEntityStore.delete(aId)

        // get deleted node
        assertNull(aEntityStore.get(aId))
    }

    @Test
    fun `test related node`() {

        val aId = "a_id"
        val bId = "b_id"

        val fromEntity = EntityType("From", A::class.java)
        val fromEntityStore = EntityStore(fromEntity)

        val toEntity = EntityType("To", B::class.java)
        val toEntityStore = EntityStore(toEntity)

        val relation = RelationType<A, Nothing, B>("relatedTo", fromEntity, toEntity, null)
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

        fromEntityStore.create(aId, a)
        toEntityStore.create(bId, b)

        // create relation
        relationStore.create(a, null, b)

        // get 'b' from 'a'
        assertEquals(listOf(b), fromEntityStore.getRelated(aId, relation, toEntity))
    }

    @Test
    fun `test relation with properties`() {

        val aId = "a_id"
        val bId = "b_id"

        val fromEntity = EntityType("From2", A::class.java)
        val fromEntityStore = EntityStore(fromEntity)

        val toEntity = EntityType("To2", B::class.java)
        val toEntityStore = EntityStore(toEntity)

        val relation = RelationType("relatedTo", fromEntity, toEntity, R::class.java)
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

        fromEntityStore.create(aId, a)
        toEntityStore.create(bId, b)

        // create relation
        val r = R()
        r.field1 = "r's value1"
        r.field2 = "r's value2"
        relationStore.create(a, r, b)

        // get 'b' from 'a'
        assertEquals(listOf(b), fromEntityStore.getRelated(aId, relation, toEntity))

        // get 'r' from 'a'
        assertEquals(listOf(r), fromEntityStore.getRelations(aId, relation))
    }

    @Test
    fun `json to map`() {
        val objectMapper = ObjectMapper()
        val map = objectMapper.readValue<Map<String, String>>("""{"label":"3GB for 300 NOK"}""", object : TypeReference<LinkedHashMap<String, String>>() {})
        assertEquals("3GB for 300 NOK", map["label"])
    }

    companion object {

        @BeforeClass
        @JvmStatic
        fun start() {
            GraphServer.start()
        }

        @AfterClass
        @JvmStatic
        fun stop() {
            GraphServer.stop()
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