package org.ostelco.prime.kts.engine.script

import org.junit.Test
import org.ostelco.prime.kts.engine.reader.ClasspathResourceTextReader
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KotlinScriptTest {

    @Test
    fun `test eval`() {
        val kts = KotlinScript(ClasspathResourceTextReader("/TestEval.kts").readText())
        kts.eval<Any?>()
        kts.eval<Any?>()
    }

    @Test
    fun `test compile and eval`() {
        val kts = KotlinScript(ClasspathResourceTextReader("/TestEval.kts").readText()).compile()
        kts.eval<Any?>()

        // Can eval only once after compiling.
        kts.eval<Any?>()
    }

    @Test
    fun `test eval with return`() {
        val result: Int? = KotlinScript(ClasspathResourceTextReader("/TestEvalWithReturn.kts").readText()).eval()
        assertEquals(123, result, "Result from eval script does not match")
    }

    @Test
    fun `test compile and eval with return`() {
        val kts = KotlinScript(ClasspathResourceTextReader("/TestEvalWithReturn.kts").readText()).compile()
        run {
            val result: Int? = kts.eval()
            assertEquals(123, result, "Result from eval script does not match")
        }
        // Can eval only once after compiling.
        run {
            val result: Int? = kts.eval()
            assertNull(result, "Result from eval script does not match")
        }
    }

    @Test
    fun `test invoke function`() {
        val kts = KotlinScript(ClasspathResourceTextReader("/TestInvokeFunction.kts").readText())

        // kts.eval<Any?>()
        // kts.eval<Any?>()

        // Invoke without eval fails.

        run {
            val result: Int = kts.invoke("add", 3, 4)

            assertEquals(7, result, "Result from function script does not match")
        }
        run {
            val result: Int = kts.invoke("add", 23, 24)
            assertEquals(47, result, "Result from function script does not match")
        }
    }

    @Test
    fun `test compile and invoke function`() {
        val kts = KotlinScript(ClasspathResourceTextReader("/TestInvokeFunction.kts").readText()).compile()

        kts.eval<Any?>()

        // Can eval only once after compiling.
        kts.eval<Any?>()

        run {
            val result = kts.invoke("add", 1, 2) as Int
            assertEquals(3, result, "Result from function script does not match")
        }
        run {
            val result = kts.invoke("add", 23, 24) as Int
            assertEquals(47, result, "Result from function script does not match")
        }
    }
}