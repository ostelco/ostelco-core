package org.ostelco.prime.jsonmapper

import org.junit.Test

class JsonMapperTest {

    @Test
    fun `test kotlin jackson module`() {
        objectMapper.readValue(
                asJson(TestDataClass(aProperty = "foo", abProperty = "bar", Name = "Vihang")),
                TestDataClass::class.java)
    }
}

data class TestDataClass(
        @JvmField val aProperty: String,
        val abProperty: String,
        @JvmField val Name: String)
