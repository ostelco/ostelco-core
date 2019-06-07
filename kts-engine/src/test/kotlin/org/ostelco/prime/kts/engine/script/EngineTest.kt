package org.ostelco.prime.kts.engine.script

import org.junit.Test
import org.ostelco.prime.kts.engine.reader.ClasspathResourceTextReader
import kotlin.test.assertEquals

class RunnableKotlinScriptTest {

    @Test
    fun `test - RunnableKotlinScript - eval`() {
        val kts = RunnableKotlinScript(ClasspathResourceTextReader("/TestEval.kts").readText())
        kts.eval<Any?>()
        kts.eval<Any?>()
    }

    @Test
    fun `test - RunnableKotlinScript - eval with return value`() {
        val kts = RunnableKotlinScript(ClasspathResourceTextReader("/TestEvalWithReturn.kts").readText())
        run {
            val result: Int? = kts.eval()
            assertEquals(123, result, "Result from eval script does not match")
        }
        run {
            val result: Int? = kts.eval()
            assertEquals(123, result, "Result from eval script does not match")
        }
    }
}

class CallableKotlinScriptTest {

    @Test
    fun `test - CallableKotlinScript - compile and invoke function`() {

        val kts = CallableKotlinScript(ClasspathResourceTextReader("/TestInvokeFunction.kts").readText())

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

class CompiledInvocableFunctionKotlinScriptTest {

    @Test
    fun `test - CompiledInvocableFunctionKotlinScript - compile and invoke function`() {

        val kts = CompiledInvocableFunctionKotlinScript(ClasspathResourceTextReader("/TestInvokeFunction.kts").readText())

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

class CompiledInvocableMethodKotlinScriptTest {

    @Test
    fun `test - CompiledInvocableMethodKotlinScript - compile and invoke method`() {

        val kts = CompiledInvocableMethodKotlinScript(ClasspathResourceTextReader("/TestInvokeMethod.kts").readText())

        run {
            val result = kts.invoke("add", 1, 2) as Int
            assertEquals(3, result, "Result from method script does not match")
        }
        run {
            val result = kts.invoke("add", 23, 24) as Int
            assertEquals(47, result, "Result from method script does not match")
        }
    }
}