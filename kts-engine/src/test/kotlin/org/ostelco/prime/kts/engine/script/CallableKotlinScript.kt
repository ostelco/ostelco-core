package org.ostelco.prime.kts.engine.script

import javax.script.Invocable
import javax.script.ScriptEngineManager

class CallableKotlinScript(private val scriptText: String) {

    private val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")

    fun <T> invoke(function: String, vararg args: Any): T {
        scriptEngine.eval(scriptText)
        return (scriptEngine as Invocable).invokeFunction(function, *args) as T
    }
}