package org.ostelco.prime.appnotifier

interface AppNotifier {
    fun notify(msisdn: String, title: String, body: String)
}
