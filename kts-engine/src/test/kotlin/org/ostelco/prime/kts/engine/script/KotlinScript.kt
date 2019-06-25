package org.ostelco.prime.kts.engine.script

import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import org.ostelco.prime.getLogger
import javax.script.CompiledScript
import javax.script.Invocable
import javax.script.ScriptEngineManager

class KotlinScript(private val scriptText: String) {

    private val logger by getLogger()

    private val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")

    private var compiledScript: CompiledScript? = null

    private var isEvaluated = false
    private var isCompiled = false
    private var isEvaluatedAfterCompiling = false

    fun <T> eval(): T? {
        if (isEvaluatedAfterCompiling) {
            logger.warn("After compiling, cannot evaluate more than once.")
            return null
        }
        if (isCompiled) {
            isEvaluatedAfterCompiling = true
        }
        isEvaluated = true
        val localCompileScript = compiledScript
        return if (localCompileScript != null) {
            localCompileScript.eval()
        } else {
            scriptEngine.eval(scriptText)
        } as T?
    }

    fun compile(): KotlinScript {
        if (!isEvaluated) {
            eval<Any?>()
        }
        compiledScript = (scriptEngine as KotlinJsr223JvmLocalScriptEngine).compile(scriptText)
        isCompiled = true
        return this
    }

    fun <T> invoke(function: String, vararg args: Any): T {
        if (!isEvaluated) {
            eval<Any?>()
        }
        val localCompileScript = compiledScript
        return if (localCompileScript != null) {
            (localCompileScript.engine as KotlinJsr223JvmLocalScriptEngine)
        } else {
            (scriptEngine as Invocable)
        }.invokeFunction(function, *args) as T
    }
}