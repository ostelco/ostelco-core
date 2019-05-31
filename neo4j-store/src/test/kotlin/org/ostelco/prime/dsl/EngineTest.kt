package org.ostelco.prime.dsl

import org.junit.Test
import kotlin.test.assertEquals

class EngineTest {

    @Test
    fun testEngine() {
        val result = KotlinScript("/Test.kts").eval()
        assertEquals(123, result, "Result from script does not match")
    }
}