package org.ostelco.prime.kts.engine

import org.ostelco.prime.kts.engine.reader.TextReader
import org.ostelco.prime.kts.engine.script.CompiledInvocableMethodKotlinScript
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class KtScriptProxy(private val kts: CompiledInvocableMethodKotlinScript) : InvocationHandler {

    override fun invoke(
            proxy: Any?,
            method: Method,
            args: Array<out Any>?): Any? {

        return if (args.isNullOrEmpty()) {
            kts.invoke(method.name)
        } else {
            kts.invoke(method.name, *args)
        }
    }

    companion object {

        fun <I> newInstance(
                interfaceClass: Class<I>,
                textReader: TextReader): I {

            return interfaceClass.cast(
                    Proxy.newProxyInstance(
                            interfaceClass.classLoader,
                            arrayOf(interfaceClass),
                            KtScriptProxy(
                                    kts = CompiledInvocableMethodKotlinScript(
                                            scriptText = textReader.readText()
                                    )
                            )
                    )
            )
        }
    }
}