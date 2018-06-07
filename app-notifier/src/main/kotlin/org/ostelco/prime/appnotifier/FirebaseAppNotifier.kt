package org.ostelco.prime.appnotifier

class FirebaseAppNotifier: AppNotifier {
    override fun notify(msisdn: String) {
        println("$msisdn notified")
    }
}