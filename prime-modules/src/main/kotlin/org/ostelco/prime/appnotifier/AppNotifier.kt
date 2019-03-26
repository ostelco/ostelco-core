package org.ostelco.prime.appnotifier

interface AppNotifier {
    fun notify(customerId: String, title: String, body: String)
}
