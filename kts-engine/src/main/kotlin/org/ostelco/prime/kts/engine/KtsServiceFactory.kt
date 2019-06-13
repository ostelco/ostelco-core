package org.ostelco.prime.kts.engine

import org.ostelco.prime.kts.engine.reader.TextReader

data class KtsServiceFactory(
        private val serviceInterface: String,
        private val textReader: TextReader) {

    fun <T> getKtsService(): T = KtScriptProxy.newInstance(Class.forName(serviceInterface), textReader) as T
}