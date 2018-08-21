package org.ostelco.tools.migration

import org.ostelco.prime.storage.firebase.FirebaseConfig
import org.ostelco.prime.storage.firebase.FirebaseConfigRegistry
import org.ostelco.prime.storage.firebase.FirebaseStorageSingleton

fun initFirebase() {
    val config = FirebaseConfig()
    config.configFile = "../../prime/config/pantel-prod.json"
    config.databaseName = "pantel-2decb"
    config.rootPath = "v2"
    FirebaseConfigRegistry.firebaseConfig = config
}

fun importFromFirebase(action: (String) -> Unit) {
    val subscribers = FirebaseStorageSingleton.subscriberStore.getAll()

    println("// Create Subscriber")
    subscribers
            .values
            .stream()
            .map { createSubscriber(it) }
            .forEach { action(it) }

    println("// Create Subscription")
    FirebaseStorageSingleton
            .balanceStore
            .getAll()
            .keys
            .stream()
            .map { createSubscription(it) }
            .forEach { action(it) }

    println("// Add Subscription to Subscriber")
    FirebaseStorageSingleton
            .subscriptionStore
            .getAll()
            .entries
            .stream()
            .map { addSubscriptionToSubscriber(it.key, it.value) }
            .forEach { action(it) }

    println("// Set balance")
    FirebaseStorageSingleton
            .balanceStore
            .getAll()
            .entries
            .stream()
            .map { setBalance(it.key, it.value) }
            .forEach { action(it) }

    println("// Add subscriber to Segment")
    subscribers
            .values
            .stream()
            .map { addSubscriberToSegment(it.email) }
            .forEach { action(it) }
}