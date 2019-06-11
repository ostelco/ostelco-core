package org.ostelco.prime.kts.engine

import org.junit.Test
import org.ostelco.prime.kts.engine.reader.ClasspathResourceTextReader
import org.ostelco.prime.kts.engine.script.CompiledInvocableMethodKotlinScript
import kotlin.test.assertEquals

interface Shape {
    fun setParam(name: String, value: Double)
    fun getArea() : Double
}

private val circle = object : Shape {

    private val kts = CompiledInvocableMethodKotlinScript(
            ClasspathResourceTextReader("/Shapes.kts").readText()
    )

    override fun setParam(name: String, value: Double) = kts.invoke<Unit>("setParam", name, value)

    override fun getArea(): Double = kts.invoke("getArea")
}

class KtScriptProxyTest {

    @Test
    fun `test - DI for Kotlin Script`() {
        circle.setParam("radius", 7.0)
        assertEquals(Math.PI * 7.0 * 7.0, circle.getArea())
    }

    @Test
    fun `test - Dynamic Proxy for Kotlin Script`() {
        val textReader = ClasspathResourceTextReader("/Shapes.kts")
        val circle = KtScriptProxy.newInstance(Shape::class.java, textReader)
        circle.setParam("radius", 7.0)
        assertEquals(Math.PI * 7.0 * 7.0, circle.getArea())
    }
}