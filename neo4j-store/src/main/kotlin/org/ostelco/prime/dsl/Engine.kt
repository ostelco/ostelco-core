package org.ostelco.prime.dsl

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

private val scriptEngine: ScriptEngine = ScriptEngineManager().getEngineByExtension("kts")

class KotlinScript(private val scriptFileName: String) {

    fun eval(): Any {
        return scriptEngine.eval({}.javaClass.getResource(scriptFileName).readText())
    }
}