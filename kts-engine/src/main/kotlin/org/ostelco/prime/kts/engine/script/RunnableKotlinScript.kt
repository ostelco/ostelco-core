package org.ostelco.prime.kts.engine.script

import javax.script.ScriptEngineManager

class RunnableKotlinScript(private val scriptText: String) {

    private val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")

    fun <T> eval(): T? {
        return scriptEngine.eval(scriptText) as T?
    }
}