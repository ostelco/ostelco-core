package org.ostelco.tools.prime.admin.actions

import org.ostelco.prime.kts.engine.reader.ClasspathResourceTextReader
import org.ostelco.prime.kts.engine.script.RunnableKotlinScript


fun setup() {
    RunnableKotlinScript(ClasspathResourceTextReader("/Setup.kts").readText()).eval<Any?>()
}

fun sync() {
    RunnableKotlinScript(ClasspathResourceTextReader("/Sync.kts").readText()).eval<Any?>()
}

fun check() {
    RunnableKotlinScript(ClasspathResourceTextReader("/Check.kts").readText()).eval<Any?>()
}