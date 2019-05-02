package org.ostelco.prime.appnotifier

interface AppNotifier {
    fun notify(customerId: String, title: String, body: String)
    fun notify(customerId: String, title: String, body: String, data: Map<String, String>)
}
