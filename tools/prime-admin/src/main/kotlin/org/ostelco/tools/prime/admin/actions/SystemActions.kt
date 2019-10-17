package org.ostelco.tools.prime.admin.actions

import org.ostelco.prime.kts.engine.reader.ClasspathResourceTextReader
import org.ostelco.prime.kts.engine.script.RunnableKotlinScript


private val scriptBaseDir = ""

fun setup() {
    RunnableKotlinScript(ClasspathResourceTextReader("$scriptBaseDir/Setup.kts").readText()).eval<Any?>()
}

fun sync() {
    RunnableKotlinScript(ClasspathResourceTextReader("$scriptBaseDir/Sync.kts").readText()).eval<Any?>()
}

fun check() {
    RunnableKotlinScript(ClasspathResourceTextReader("$scriptBaseDir/Check.kts").readText()).eval<Any?>()
}

fun index() {
    RunnableKotlinScript(ClasspathResourceTextReader("$scriptBaseDir/Index.kts").readText()).eval<Any?>()
}
