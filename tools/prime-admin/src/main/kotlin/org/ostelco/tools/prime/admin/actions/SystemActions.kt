package org.ostelco.tools.prime.admin.actions

import org.ostelco.prime.dsl.KotlinScript

fun setup() {
    KotlinScript("/Setup.kts").eval()
}